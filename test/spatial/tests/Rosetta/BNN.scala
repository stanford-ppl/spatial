import spatial.dsl._
import spatial.targets._
import utils.implicits._

@spatial class BNN extends SpatialTest {

  // -------------------------------------------------
  // Params
  // -------------------------------------------------

  val par_3x3_inner = 8
  val par_3x3_outer = 1
  val par_fc_inner = 16
  val par_fc_outer = 2

  val par_fp_outer = 2
  
  val SIZE = 32 // row size 
  val B_3x3 = 1   // Block size for 3x3 convolution
  val BATCH_SIZE = 1000

  val par_load_1  = 8
  val par_store_1 = 8 

  val nw = 3
  // -------------------------------------------------
  // End of params
  // -------------------------------------------------  
  
  type T   = FixPt[TRUE,_12,_20]
  type TB  = Bit 


  // type size for binary weight kernels
  type FPB = UInt32
  type FCB = UInt16

  type TS  = FixPt[TRUE,_8,_8]
  type WB  = UInt4

  val bin_weight_size = 8
  val bin_weight_size_fpb = 32

  val fc_max_in_channels  = 8192 // Note: This is aligned because of the 512
  val fc_max_out_channels = 1024
  
  val conv_max_in_channels  = 256
  val conv_max_out_channels = 512 
  val conv_min_in_channels  = 128 

  val output_total_channels = 10

  val conv_L = 5
  val fc_L   = 3

  def main(args: Array[String]): Void = {
  
    // val conv_temp_DRAM      = DRAM[TB](BATCH_SIZE, conv_L, conv_max_out_channels, SIZE, SIZE)   //DRAM[T](BATCH_SIZE, 1024, SIZE, SIZE)
 
  	val img_DRAM 	    	= DRAM[T](BATCH_SIZE, 3, SIZE, SIZE) // 3-channel 32 x 32 image 
  	val fp_weight_DRAM  = DRAM[TB](3, 3, 128) // 3-channel 9-bit string (3x3) 

   	val conv_weight_DRAM  = DRAM[TB](fc_L, conv_max_out_channels, 3,3, conv_max_in_channels)     //DRAM[TB](4224, 512, 9)

  	val fc_weight_DRAM	  = DRAM[TB](fc_L, fc_max_out_channels, fc_max_in_channels)
  	val out_DRAM 	       	= DRAM[TB](BATCH_SIZE, 2, fc_max_in_channels)

    // Initialize arrays - Note there is no bias in BNN's
    val input      = (0::BATCH_SIZE, 0::3, 0::SIZE, 0::SIZE) { (i,j,k,l) => 0.to[T] }    
    val fp_weights = (0::3, 0::3, 0::128) { (q,l,w) => 0.to[TB] } // 16 is 3x3 = 9, rounded up to the next alignment
    val fc_weights = (0::fc_L, 0::fc_max_out_channels, 0::fc_max_in_channels) { (i,j,k) => 0.to[TB] }
   
   	setMem(img_DRAM, input)  
   	setMem(fp_weight_DRAM, fp_weights)
   	setMem(fc_weight_DRAM, fc_weights)

    //other values for pooling
    //TODO: these are random values --> need to be replcaed with actual model values
    val mu    = 0.2
    val theta = 0.2
    val gamma = 0.4
    val beta  = 0.5
    val e     = 0.12

    val k = (gamma / ( sqrt(theta*theta + e) )).to[TS]
    val h = (beta - ( mu * gamma) / (sqrt(theta*theta + e) )).to[TS]
 
    val BATCH_SIZE_USERDEF = args(0).to[Int]
    val outd = ArgOut[T]

  	Accel {

      /* Load some weights first */
      // 3x3x3 filter represent by 32-bit vector 
      val fp_all_weights = SRAM[TB](3, 3, 128)
      fp_all_weights load fp_weight_DRAM(0::3, 0::3, 0::128 par 8)

     // val fc_all_layer_values = SRAM[TB](2, 128, SIZE, SIZE).buffer 
      val fc_all_layer_values = SRAM[TB](2, conv_max_out_channels, SIZE, SIZE) 

      /* Rest of BNN */
      def batch_norm(conv_result : T) : TB = {
        val norm_comp = k * conv_result.to[TS] + h
        mux(norm_comp >= 0, 1.to[TB], 0.to[TB])
      }

  		def fp_conv3x3_norm(batch_img : Int) : Unit = {

  			val nr = SIZE 
  			val nc = SIZE 
  			val or = SIZE 
  			val oc = SIZE 

  			val in_channels   = 3
  			val out_channels  = 128

  			val max_nr = SIZE 
  			val max_nc = SIZE 
  			val max_or = SIZE  
  			val max_oc = SIZE 

  			val max_out_channels = 128

        val fp_rgb_image_lb  = List.fill(in_channels) { LineBuffer[T](nw, max_nr)  }

  			//Conv2D 
  			val B = 1
        // assumes --> [B8 .... B0][G8 ... G0][R8   ...  R0] in bit vector format 
        Foreach(0 until B, 0 until  nr+1) { (b,r) =>

          Parallel {
            List.tabulate(in_channels) { k =>
              fp_rgb_image_lb(k) load img_DRAM(batch_img, k.to[I32], r :: r+1, 0::nc par par_load_1)
            }
          } 

          // A block of 2D outputs
          //val result        = SRAM[TB](max_nc)

          if (r >= 1) {
            Foreach(max_out_channels by B, 0 until nc+1 by 1) { (outD_i, c) =>
              val ct = c - 1
              val rt = r - 1
              val do_set_to_0 = ct < nw - 2 || ct >= max_oc - 1 || rt < nw - 2 || rt >= max_or - 1

              val conv_3x3_result = List.tabulate(in_channels,nw,nw) { (k,rw,cw) =>
                                       val curr_bin_weight = fp_all_weights(rw, cw, outD_i)
                                       val img_lb          = fp_rgb_image_lb(k)
                                       
                                       val actual_cw       = ct + cw
                                       val curr_pix        = img_lb(rw, actual_cw)

                                      /* Sign-Inversion in FP conv stage for BNN's (replaces multiply-add convolution) */ 
                                       val fp_pix = mux(curr_bin_weight == 0.to[TB], curr_pix, -curr_pix)
                                       mux(do_set_to_0, 0.to[T], fp_pix)
                                    }.flatten.flatten.reduceTree(_ + _)

              //perform batch norm and binarization
              val c_result = mux(c >= 1, c-1, 0)
              fc_all_layer_values(0.to[I32], outD_i, r-1, c_result) = batch_norm(conv_3x3_result) 
            }
          }          
        }
			}

//    def conv3x3_pool(batch_img : Int, 
//                     L : Int) : Unit = {

//       val nr_LUT              = LUT[Int](5)( SIZE,   SIZE/2, SIZE/2, SIZE/4, SIZE/4)
//       val or_LUT              = LUT[Int](5)( SIZE/2, SIZE/2, SIZE/4, SIZE/4, SIZE/8)
//    
//       val in_channels_LUT     = LUT[Int](5)( 128, 128, 256, 256, 512)
//       val out_channels_LUT    = LUT[Int](5)( 128, 256, 256, 512, 512)

//       val pool_size = 2

//       val nr = nr_LUT(L)
//       val nc = nr_LUT(L)
//       val or = or_LUT(L)
//       val oc = or_LUT(L)

//       val in_channels  = in_channels_LUT(L)
//       val out_channels = 1 //out_channels_LUT(L)

//       // For SRAM sizes and aligned loads/stores
//       val max_nr = SIZE
//       val max_nc = SIZE
//       val max_or = SIZE
//       val max_oc = SIZE
//       val max_out_channels = 512

//       // Conv2D
//       Foreach(out_channels by 1) { (outD_i) => 
//         val tmp_SRAM_conv = SRAM[T](max_nr, max_nc)

//         // Load a 3x3 kernel for current out_channel 
//         val bin_weight_SRAM = SRAM[TB](nw, nw, conv_max_in_channels) // 3 x 3 x 512 
//         bin_weight_SRAM load conv_weight_DRAM(L, outD_i, 0::3, 0::3, 0::in_channels par 16)

//         Pipe { 
//           // Perform convolution 
//           Foreach(0 until nr, 0 until nc) { (i,j) => 

//           //val conv_3x3_L_value = Reduce(Reg[T](0.to[T]))(in_channels by 1 par 32) { (inD_i) =>
//           //                          val conv_win_3x3_xnor = Seq.tabulate(2, 2) { (ri, ci) =>
//           //                                                    !(fc_all_layer_values(L%2, inD_i, i + ri, j + ci) ^ bin_weight_SRAM(ri, ci, inD_i)).to[TB]
//           //                                                  }.flatten
//           //                          popcount(conv_win_3x3_xnor).to[T]                                         
//           //                        //  fc_all_layer_values(L%2, inD_i, 0.to[I32], 0.to[I32]).to[T]
//           //                       }{_ + _}
//             val tmp_fc_SRAM = SRAM[TB](nw, nw, conv_max_in_channels)
//             Foreach(nw by 1, nw by 1, in_channels by 1 par 64) { (ri, ci, inD_i) =>
//               tmp_fc_SRAM(ri, ci, inD_i) = fc_all_layer_values(L%2, inD_i, i + ri, j + ci)
//             }

//             val conv_3x3_L_value = Reduce(Reg[T](0.to[T]))(nw by 1, nw by 1, in_channels by 64) { (ri, ci, inD_i) =>
//                                       val conv_win_xnor = Seq.tabulate(64) { p => 
//                                                             //  !(fc_all_layer_values(L%2, inD_i + p, i + ri, j + ci) ^ bin_weight_SRAM(ri, ci, inD_i + p)).to[TB]
//                                                            // !(tmp_fc_SRAM(ri, ci, inD_i + p) ^ bin_weight_SRAM(ri, ci, inD_i + p)).to[TB]
//                                                             1.to[TB]
//                                                           }
//                                       //popcount(conv_win_xnor).to[T]
//                                       conv_win_xnor(0).to[T]
//                                    }{_ + _}

//             tmp_SRAM_conv(i, j) = conv_3x3_L_value
//           }

//           // Perform max-pooling 
//           Foreach(or by 1, oc by 1) { (i,j) =>
//             val output_current = tmp_SRAM_conv(i, j)
//             val output_bpooled = List.tabulate(pool_size, pool_size) { (pool_i, pool_j) =>
//                                    tmp_SRAM_conv(i*pool_size + pool_i, j*pool_size + pool_j) 
//                                  }.flatten.reduceTree{(a,b) => max(a,b)}     

//             //do pooling/batch normalization when L is odd 
//             val actual_layer_output = mux(L%2.to[Int] == 0.to[Int], output_current, output_bpooled)

//             val ri = i //+ (outD_lo * out_offset) 
//             val cj = j 
//             fc_all_layer_values(1-L%2, outD_i, ri, cj) = batch_norm(output_bpooled) 
//           }
//         }
//      }

//    }

      def conv3x3_pool(batch_img : Int, 
                       L : Int) : Unit = {

        val nr_LUT              = LUT[Int](5)( SIZE,   SIZE/2, SIZE/2, SIZE/4, SIZE/4)
        val or_LUT              = LUT[Int](5)( SIZE/2, SIZE/2, SIZE/4, SIZE/4, SIZE/8)
     
        val in_channels_LUT     = LUT[Int](5)( 128, 128, 256, 256, 512)
        val out_channels_LUT    = LUT[Int](5)( 128, 256, 256, 512, 512)
       
        val in_offset_LUT     = LUT[Int](5)( 128/conv_min_in_channels, 128/conv_min_in_channels, 256/conv_min_in_channels,
                                             256/conv_min_in_channels, 512/conv_min_in_channels)
        val out_offset_LUT    = LUT[Int](5)( 128/conv_min_in_channels, 256/conv_min_in_channels, 256/conv_min_in_channels,
                                             512/conv_min_in_channels, 512/conv_min_in_channels)

        //val weight_offset_LUT   = LUT[Int](5)( 128, 256, 384, 640, 896)
        
        // Parameters for this layer
        val pool_size = 2

        val nr = nr_LUT(L)
        val nc = nr_LUT(L)
        val or = or_LUT(L)
        val oc = or_LUT(L)

        val in_channels  = in_channels_LUT(L)
        val out_channels = out_channels_LUT(L)

        // For SRAM sizes and aligned loads/stores
        val max_nr = SIZE
        val max_nc = SIZE
        val max_or = SIZE
        val max_oc = SIZE
        val max_out_channels = 512

        val in_offset  = in_offset_LUT(L)
        val out_offset = out_offset_LUT(L)

        // Conv2D
        val B = B_3x3
        Foreach(out_channels by 1) { outD_i => 
         
          val tmp_SRAM_conv = SRAM[T](max_nr, max_nc)

          // Load a 3x3 kernel for current out_channel 
          val bin_weight_SRAM = SRAM[TB](nw, nw, conv_max_in_channels)
          bin_weight_SRAM load conv_weight_DRAM(L, outD_i, 0::3, 0::3, 0::in_channels par 16)

          val L_inx = L%2
          Pipe{
            // XOR ALU in Bin conv stage for BNN's (replaces multiply-add convolution) 
           MemReduce(tmp_SRAM_conv)(in_channels by 16 par par_3x3_inner) { inD_i =>
             // in channels
             // A block of 2D outputs
             val result = SRAM[T](max_nr, max_nc)
        //     Foreach(0 until nr, 0 until nc) { (r,c) =>
        //     val row_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-r   )) // If I don't use .to[Int] here it has Spatial errors
        //     val row_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nr-r))
        //     val col_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-c   ))
        //     val col_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nc-c))
        //     val window    = Reduce(Reg[T](0.to[T]))(row_start until row_end, col_start until col_end par 3){ (i,j) =>
        //                         val cv_ri = (r-1+i) + li * nc
        //                         val cv_ci = (c-1+j) //+ li * nr
        //                         val img2D_bnn_fp = fc_all_layer_values(L%2, inD_i, cv_ri, cv_ci) //TODO: double check again
        //                         val conv_result = !(img2D_bnn_fp ^ bin_weight_SRAM(i, j, inD_i))
        //                         conv_result.to[T]
        //                     }{_+_}

        //       val conv_win_xnor = Seq.tabulate(1, 1) { (ri,ci) =>  
        //                               !(fc_all_layer_values(L_inx, inD_i, ri + r, ci + c) ^ bin_weight_SRAM(ri + r, ci + c, inD_i)).to[TB]                                                         
        //                           }.flatten
               Foreach(0 until nr, 0 until nc) { (r,c) =>

                  val conv_total_value =  Reduce(Reg[T](0.to[T]))(nw by 1, nw by 1) { (i,j) =>                  
                                             val conv_win_xnor = Seq.tabulate(16) { p => fc_all_layer_values(L_inx, inD_i + p, r + i, c + j) }
                                             popcount(conv_win_xnor).to[T]
                                          }{_ + _}
                 //  result(r, c) = r.to[T] //fc_all_layer_values(L_inx, inD_i, r, c).to[T]
                   result(r, c) = conv_total_value //popcount(conv_win_xnor).to[T]
               }
             result
           }{_+_} // Reduce across in channels

     //     val result_SRAMs = List.fill(par_3x3_inner) { SRAM[T](max_nr, max_nc) } 
     //     Foreach(in_channels by 1 by par_3x3_inner) { inD_i =>  
     //       Parallel{ 
     //         List.tabulate(par_3x3_inner) { p =>
     //           Foreach(0 until nr, 0 until nc) { (r,c) =>
     //              result_SRAMs(p)(r,c) = r.to[T] + result_SRAMs(p)(r,c)
     //           }
     //         }
     //       }
     //     }

     //     Foreach(0 until nr, 0 until nc) { (r,c) =>
     //       tmp_SRAM_conv(r,c) = Seq.tabulate(par_3x3_inner){ p =>  result_SRAMs(p)(r,c) }.reduceTree(_+_)
     //     }
            //Max-pooling layer 2x2
            // Fused Pool
            //or and oc already take into account size of layer after max pooling
            Foreach(or by 1, oc by 1) { (i,j) =>
              val output_current = tmp_SRAM_conv(i, j)
              val output_bpooled = List.tabulate(pool_size, pool_size) { (pool_i, pool_j) =>
                                     tmp_SRAM_conv(i*pool_size + pool_i, j*pool_size + pool_j) 
                                   }.flatten.reduceTree{(a,b) => max(a,b)}     

              //do pooling/batch normalization when L is odd 
              val actual_layer_output = mux(L%2.to[Int] == 0.to[Int], output_current, output_bpooled)

           //  val outD_li = outD_i / out_offset
           //  val outD_lo = outD_i % out_offset
              val ri = i // + (outD_lo * out_offset) 
              val cj = j 
              fc_all_layer_values(1-L_inx, outD_i, ri, cj) = batch_norm(output_bpooled) 
            }
          }
        }  
      }


  		/* Start of BNN accelerator */
  		Sequential.Foreach(BATCH_SIZE_USERDEF by 1) { batch_img => 

  			// FP-Conv (1)
  			fp_conv3x3_norm(batch_img)

  			//Bin-Conv-Layer (2)
  		  Foreach(0 until 1) { L =>
		   	  conv3x3_pool(batch_img, L)  				
    		} 

        val tmp_SRAM_fc = SRAM[TB](2, fc_max_in_channels)

        val output_dim = SIZE/8

        val fc_in_channels     = LUT[Int](3)( 8192, 1024, 1024 )
        val fc_out_channels    = LUT[Int](3)( 1024, 1024, 10 )

        val reduce_bit_par = 64
        //Bin-Fully-Connected-Layer (3)
        Pipe { 
          Foreach(conv_max_out_channels by 1, output_dim by 1, output_dim by 1 par output_dim) { (ch, r, k) =>
              tmp_SRAM_fc(0.to[I32], ch*16 + r*output_dim + k) = fc_all_layer_values(1.to[I32], ch, r, k)
          }

    /*      Foreach(0 until 3) { L =>          
            val fc_in_channels_c = fc_in_channels(L)
            // Note: Can block this loop if needed
            Foreach(fc_out_channels(L) by 1 par par_fc_outer) { out_i =>
              // Load 1 row of weight matrix
              val weight_SRAM = SRAM[TB](fc_max_in_channels)
              weight_SRAM load fc_weight_DRAM(L, out_i, 0::fc_in_channels_c par 64)

              // XNOR + Popcount op in lieu of dot product
              Pipe {
                val L_inx = L%2
                val prod = Reduce(Reg[T](0.to[T]))(fc_in_channels_c by reduce_bit_par){ in_i =>
                              val seq_prod_tmp = Seq.tabulate(reduce_bit_par) { p =>
                                !(tmp_SRAM_fc(L_inx, in_i + p) ^ weight_SRAM(in_i + p))
                              }
                              popcount(seq_prod_tmp).to[T] //popcount here 
                           }{_+_}
                tmp_SRAM_fc(1-L_inx, out_i) = batch_norm(prod.value).to[TB] 
              }
            }
          }     */   

          // out_DRAM(batch_img, 0::2, 0::fc_max_in_channels par 8) store tmp_SRAM_fc
          outd := tmp_SRAM_fc(0.to[I32], 0.to[I32]).to[T]
        }
  		} 

  	}

	  val out_bnn = getTensor3(out_DRAM)
	  //printTensor3(out_bnn)


  }

}