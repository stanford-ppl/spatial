import spatial.dsl._
import spatial.targets._

@spatial class BNN extends SpatialTest {

  // -------------------------------------------------
  // Params
  // -------------------------------------------------

  val par_3x3_inner = 4
  val par_3x3_outer = 1
  val par_fc_inner = 16
  val par_fc_outer = 1
  
  val SIZE = 32 // row size 
  val B_3x3 = 1   // Block size for 3x3 convolution
  val BATCH_SIZE = 1000

  val par_load_1  = 16
  val par_store_1 = 16 

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
  	val fp_weight_DRAM  = DRAM[TB](128, 3, 3) // 3-channel 9-bit string (3x3) 

   	val conv_weight_DRAM   	= DRAM[TB](fc_L, conv_max_out_channels, conv_max_in_channels,3,3)     //DRAM[TB](4224, 512, 9)

  	val fc_weight_DRAM	    = DRAM[TB](fc_L, fc_max_out_channels, fc_max_in_channels)
  	val out_DRAM 		= DRAM[TB](BATCH_SIZE, 2, fc_max_in_channels)

    // Initialize arrays - Note there is no bias in BNN's
    val input      = (0::BATCH_SIZE, 0::3, 0::SIZE, 0::SIZE) { (i,j,k,l) => 0.to[T] }    
    val fp_weights = (0::128, 0::3, 0::3) { (q,l,w) => 0.to[TB] } // 16 is 3x3 = 9, rounded up to the next alignment
    val fc_weights = (0::fc_L, 0::fc_max_out_channels, 0::fc_max_in_channels) { (i,j,k) => 0.to[TB] }
   
   	setMem(img_DRAM, input)  
   	setMem(fp_weight_DRAM, fp_weights)
   	setMem(fc_weight_DRAM, fc_weights)

    //other values for pooling
    val mu    = 0.2
    val theta = 0.2
    val gamma = 0.4
    val beta  = 0.5
    val e     = 0.12

    val k = (gamma / ( sqrt(theta*theta + e) )).to[TS]
    val h = (beta - ( mu * gamma) / (sqrt(theta*theta + e) )).to[TS]

    val BATCH_SIZE_USERDEF = args(0).to[Int]

  	Accel {

      /* Load some weights first */
      // 3x3x3 filter represent by 32-bit vector 
      val fp_all_weights = SRAM[TB](128, 3, 3)
      //fp_all_weights load fp_weight_DRAM(0::128, 0::3, 0::3)

      val fc_all_layer_values = SRAM[TB](2, 128, SIZE, SIZE)

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
  			Foreach(out_channels by B) { outD_i =>	
       
		      // A block of 2D outputs
		      val result        = SRAM[TB](max_nc)

          // assumes --> [B8 .... B0][G8 ... G0][R8   ...  R0] in bit vector format 
          Foreach(0 until B, 0 until  nr+1) { (b,r) =>

            Parallel {
              List.tabulate(in_channels) { k =>
                fp_rgb_image_lb(k) load img_DRAM(batch_img, k.to[I32], r :: r+1, 0::nc par par_load_1)
              }
            } 

            Foreach(0 until nc+1) { c =>
              val ct = c - 1
              val rt = r - 1
              val do_set_to_0 = ct < nw - 2 || ct >= max_oc - 1 || rt < nw - 2 || rt >= max_or - 1

              val conv_3x3_result = List.tabulate(in_channels,nw,nw) { (k,rw,cw) =>
                                       val curr_bin_weight = fp_all_weights(outD_i, rw, cw)
                                       val img_lb   = fp_rgb_image_lb(k)
                                       
                                       val actual_cw       = ct + cw
                                       val curr_pix        = img_lb(rw, actual_cw)

                                      /* Sign-Inversion in FP conv stage for BNN's (replaces multiply-add convolution) */ 
                                       val fp_pix = mux(curr_bin_weight == 0.to[Bit], curr_pix, -curr_pix)
                                       mux(do_set_to_0, 0.to[T], fp_pix)
                                    }.flatten.flatten.reduceTree(_ + _)

              //perform batch norm and binarization
              result(mux(c >= 1, c-1, 0)) = batch_norm(conv_3x3_result)
            }

            if (r >= 1) {
              Foreach(0 until oc par 16) { c =>
                fc_all_layer_values(0.to[I32], outD_i, r-1, c) = result(c) 
              }
            }

          }

  			}

  		}


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
        Sequential.Foreach(out_channels by B par par_3x3_outer) { outD_i => 
         
          val tmp_SRAM_conv = SRAM[T](B, max_nr, max_nc)

          // Load a 3x3 kernel for current out_channel 
          val bin_weight_SRAM = SRAM[TB](B, conv_max_in_channels, nw, nw)
          bin_weight_SRAM load conv_weight_DRAM(L, outD_i :: outD_i + B, 0:: in_channels, 0::3, 0::3)

          // XOR ALU in Bin conv stage for BNN's (replaces multiply-add convolution) 
          MemReduce(tmp_SRAM_conv)(conv_min_in_channels by 1, in_offset by 1 par par_3x3_inner) { (inD_i, li) =>
          // in channels
            // A block of 2D outputs
            val result = SRAM[T](B, max_nr, max_nc)

            Foreach(0 until B, 0 until nr, 0 until nc) { (b,r,c) =>
              val row_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-r   )) // If I don't use .to[Int] here it has Spatial errors
              val row_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nr-r))
              val col_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-c   ))
              val col_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nc-c))
              val window    = Reduce(Reg[T](0.to[T]))(row_start until row_end, col_start until col_end par 3){ (i,j) =>
                                  val cv_ri = (r-1+i) + li * nc
                                  val cv_ci = (c-1+j) //+ li * nr
                                  val img2D_bnn_fp = fc_all_layer_values(L%2, inD_i, cv_ri, cv_ci) //TODO: double check again
                                  val conv_result = !(img2D_bnn_fp ^ bin_weight_SRAM(b, inD_i, i, j))
                                  conv_result.to[T]
                              }{_+_}
              result(b, r, c) = window.value
            }
            result
          }{_+_} // Reduce across in channels

          //Max-pooling layer 2x2
          //do pooling/batch normalization when L is odd 
          val B_pool = mux(L % 2.to[Int] == 0.to[Int], 0.to[Int], B) 
          //val tmp_SRAM_pool = SRAM[T](max_or, max_oc)

          // Fused Pool
          //or and oc already take into account size of layer after max pooling
          Foreach(or by 1, oc by 1) { (i,j) =>
            val output_bpooled = Reg[T](0.to[T])
            output_bpooled := tmp_SRAM_conv(0.to[I32], i, j)
            Foreach(B_pool by 1) { b =>
              output_bpooled :=  List.tabulate(pool_size, pool_size) { (pool_i, pool_j) =>
                                    tmp_SRAM_conv(b, i*pool_size + pool_i, j*pool_size + pool_j) 
                                 }.flatten.reduce{(a,b) => max(a,b)}                                  
            }
            val outD_li = outD_i / out_offset
            val outD_lo = outD_i % out_offset
            val ri = i + (outD_lo * out_offset) 
            val cj = j 
            fc_all_layer_values(1-L%2, outD_li, ri, cj) = batch_norm(output_bpooled.value) 
          }

        }  
      }

  		/* Start of BNN accelerator */
  		Sequential.Foreach(BATCH_SIZE_USERDEF by 1) { batch_img => 

  			// FP-Conv
  			fp_conv3x3_norm(batch_img)

  			//Bin-Conv-Layer 
  //		Foreach(0 until 5) { L =>
	//	   	conv3x3_pool(batch_img, L)  				
  //		}

        //Bin-Fully-Connected-Layer 
        val tmp_SRAM_fc = SRAM[TB](2, fc_max_in_channels)

        val output_dim = SIZE/8
        val curr_L = 5
        //512 = 128 * 4 
        Foreach(128 by 1, 16 by 1) { (ch, r) =>
            //row load conv_temp_DRAM(batch_img, curr_L.to[I32], j, i, 0::4 par 4) 
            // This should be 0::(SIZE/32) but then won't be aligned
            Foreach(output_dim by 1 par output_dim) { k =>
              tmp_SRAM_fc(0, ch*16 + r*output_dim + k) = fc_all_layer_values(1.to[I32], ch, r, k)
            }
        }
        
       val fc_in_channels     = LUT[Int](3)( 8192, 1024, 1024 )
       val fc_out_channels    = LUT[Int](3)( 1024, 1024, 10 )

       Foreach(0 until 3) { L =>          
         val fc_in_channels_c = fc_in_channels(L)
         // Note: Can block this loop if needed
         Foreach(fc_out_channels(L) by 1 par par_fc_outer) { out_i =>
           // Load 1 row of weight matrix
           val weight_SRAM = SRAM[TB](fc_max_in_channels)
           //weight_SRAM load fc_weight_DRAM(L, out_i, 0::fc_in_channels_c)

           // XOR op in lieu of dot product
           Pipe {
             val prod = Reduce(Reg[T](0.to[T]))(fc_in_channels_c by 1 par par_fc_inner){ in_i =>
                           val prod_tmp = !(tmp_SRAM_fc(L%2, in_i) ^ weight_SRAM(in_i))
                           prod_tmp.to[T] //popcount here 
                        }{_+_}
             tmp_SRAM_fc(1-L%2, out_i) = batch_norm(prod.value).to[TB] 
           }
         }

       } 
       out_DRAM(batch_img, 0::2, 0::fc_max_in_channels par par_store_1) store tmp_SRAM_fc

  		}

  	}

	val out_bnn = getTensor3(out_DRAM)
	//printTensor3(out_bnn)


  }

}