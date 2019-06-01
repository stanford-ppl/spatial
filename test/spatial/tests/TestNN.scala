import spatial.dsl._
import spatial.targets._
import utils.implicits._

@spatial class TestNN extends SpatialTest {

  // -------------------------------------------------
  // Params
  // -------------------------------------------------

  
  val SIZE = 32 // row size 

  val par_bin_out = 1

  val nw = 3

  val par_fc_outer = 1
  val par_fc_inner = 2

  val par_load_1 = 8

  // -------------------------------------------------
  // End of params
  // -------------------------------------------------  
  
  type T   = FixPt[TRUE,_12,_20]
  type TB  = UInt64 //Bit 

  type TS  = FixPt[TRUE,_8,_8]
  type WB  = UInt4

  val conv_max_in_channels  = 512/64
  val conv_max_out_channels = 512

  val fc_max_in_channels  = 8192/64 // Note: This is aligned because of the 512
  val fc_max_out_channels = 1024

  val max_nr = SIZE
  val max_nc = SIZE

  val fp_max_weights = 128 //Do use Bit type here for First Layer

  val BATCH_SIZE = 1000

  def main(args: Array[String]): Void = {
  
      val img_DRAM        = DRAM[T](BATCH_SIZE, 3, SIZE, SIZE) // 3-channel 32 x 32 image 

      val conv_weight_DRAM  = DRAM[TB](5, conv_max_out_channels, nw, nw, conv_max_in_channels)     //DRAM[TB](4224, 512, 9)
      val fc_weight_DRAM    = DRAM[TB](3, fc_max_out_channels * fc_max_in_channels)
    
      val fp_weight_DRAM    = DRAM[Bit](nw, nw, fp_max_weights) // 3-channel 9-bit string (3x3) 

      val outDRAM = ArgOut[Bit]
      val BATCH_SIZE_USERDEF = args(0).to[Int]
  
      val mu    = 0.2
      val theta = 0.2
      val gamma = 0.4
      val beta  = 0.5
      val e     = 0.12

      val k = (gamma / ( sqrt(theta*theta + e) )).to[TS]
      val h = (beta - ( mu * gamma) / (sqrt(theta*theta + e) )).to[TS]

      val flayer = args(1).to[Int]
      val slayer = args(2).to[Int]

      /* Initialize input */
      val input           = (0::BATCH_SIZE, 0::3, 0::SIZE, 0::SIZE) { (i,j,k,l) => k.to[T] * l.to[T] }    
      val fp_weight_input = (0::nw, 0::nw, 0::fp_max_weights) { (i,j,k) => if (i==1 && j == 1) 0.to[Bit] else 1.to[Bit] }

      setMem(img_DRAM, input) 
      setMem(fp_weight_DRAM, fp_weight_input)

      Accel {

        val fc_layer_conv = SRAM[TB](2, SIZE, SIZE, 512/64)
        
        val fp_all_weights = SRAM[Bit](nw, nw, fp_max_weights)
        fp_all_weights load fp_weight_DRAM(0::nw, 0::nw, 0::fp_max_weights par 8)

        val out_results_SRAM = SRAM[Bit](BATCH_SIZE, 10)

        def batch_norm(conv_result : T) : Bit = {
          val norm_comp = k * conv_result.to[TS] + h
          mux(norm_comp >= 0, 1.to[Bit], 0.to[Bit])
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

          val fp_layer_bit = SRAM[Bit](SIZE, SIZE, max_out_channels)

          val fp_rgb_image_lb  = List.fill(in_channels) { LineBuffer[T](nw, max_nr)  }

          //Conv2D 
          // assumes --> [B8 .... B0][G8 ... G0][R8   ...  R0] in bit vector format 
          Foreach(0 until  nr+1) { r =>

            Parallel {
              List.tabulate(in_channels) { k =>
                fp_rgb_image_lb(k) load img_DRAM(batch_img, k.to[I32], r :: r+1, 0::nc par par_load_1)
              }
            } 

            // A block of 2D outputs
            if (r >= 1) {
              Foreach(max_out_channels by 1, 0 until nc by 1) { (outD_i, c) =>
                val rt = r - 1
                val conv_3x3_result = List.tabulate(in_channels,nw,nw) { (k,rw,cw) =>
                                         val curr_bin_weight = fp_all_weights(rw, cw, outD_i)
                                         val img_lb          = fp_rgb_image_lb(k)
                                         
                                         val ct  = c + cw - 1
                                         val curr_pix = img_lb(rw, ct)
                                         
                                         val do_set_to_0 = ct < 0 || ct >= max_oc || rt < nw - 2 || rt >= max_or - 1

                                        /* Sign-Inversion in FP conv stage for BNN's (replaces multiply-add convolution) */ 
                                         val fp_pix = mux(curr_bin_weight == 0.to[Bit], curr_pix, -curr_pix)
                                         mux(do_set_to_0, 0.to[T], fp_pix)
                                      }.flatten.flatten.reduceTree(_ + _)

                //perform batch norm and binarization
                fp_layer_bit(r-1, c, outD_i) = batch_norm(conv_3x3_result) 
              }
            }          
          }

          Foreach(nr by 1, nc by 1, 2 by 1 par 2) { (r,c,d) =>
            fc_layer_conv(0.to[I32], r, c, d) = catSeq(Seq.tabulate(64){p=> fp_layer_bit(r,c,d*64 + p)}).as[TB]
          }
        }

        def conv3x3_pool(batch_img : Int, L : Int) : Unit = {

            val nr_LUT              = LUT[Int](5)( SIZE,   SIZE/2, SIZE/2, SIZE/4, SIZE/4)
            val or_LUT              = LUT[Int](5)( SIZE/2, SIZE/2, SIZE/4, SIZE/4, SIZE/8)
         
            val in_channels_LUT     = LUT[Int](5)( 128/64, 128/64, 256/64, 256/64, 512/64)
            val out_channels_LUT    = LUT[Int](5)( 128, 256, 256, 512, 512)
           
            val conv_iters_LUT   = LUT[Int](5)(1, 1, 2, 2, 4)
            
            // Parameters for this layer
            val pool_size = 2
            val par_conv  = 4

            val nr = nr_LUT(L)
            val nc = nr_LUT(L)
            val or = or_LUT(L)
            val oc = or_LUT(L)

            val rt = conv_iters_LUT(L)

            val in_channels  = in_channels_LUT(L)
            val out_channels = out_channels_LUT(L)

            val fc_layer_conv_bit = SRAM[Bit](nw, nw, 512)
            Foreach(out_channels by 1 par par_bin_out) { outD_i =>
              val tmp_SRAM_conv = SRAM[T](max_nr, max_nc)

              // Load a 3x3 kernel for current out_channel 
              val bin_weight_SRAM = RegFile[TB](nw, nw, conv_max_in_channels)
              bin_weight_SRAM load conv_weight_DRAM(L, outD_i, 0::nw, 0::nw, 0::in_channels par 2)
              
              //Pipe{
                //val conv_weight_rf = RegFile[TB](nw, nw, 2)                  
                Foreach(nr by 1, nc by 1, rt by 1) { (r,c,di) =>

                    val conv_layer_values = RegFile[T](par_conv)
                    val conv_xnor_win =  List.tabulate(nw,nw,2) { (i, j, inD_p) =>
                                            val inD_i = di * 2 + inD_p 
                                            val r_inx = r+i-1
                                            val c_inx = c+j-1
                                            val out_of_img_bounds = r_inx < 0.to[I32] || c_inx < 0.to[I32] || r_inx > (nr-1) || c_inx > (nc-1)
                                            val conv_xnor_win = ~(bin_weight_SRAM(i, j, inD_i) ^ fc_layer_conv(L%2, r_inx, c_inx, inD_i)) 
                                            val pcnt_conv = conv_xnor_win //popcount(Seq.tabulate(64){ p => conv_xnor_win.bit(p).to[Bit] } ).to[T]
                                            mux(out_of_img_bounds, 0.to[T], pcnt_conv.to[T])
                                          }.flatten.flatten.reduceTree(_+_)

                    conv_layer_values(di) = conv_xnor_win  
                    if (di == rt - 1) {
                      tmp_SRAM_conv(r, c) = List.tabulate(par_conv) { rf => mux(rf > rt, 0.to[T], conv_layer_values(rf)) }.reduceTree(_+_)
                    }
                }   
                //fc_layer_conv(1-L%2, 0.to[I32], 0.to[I32], outD_i) = tmp_SRAM_conv(0.to[I32], 0.to[I32]).to[TB]

                Foreach(or by 1, oc by 1) { (i,j) =>
                  val output_current = tmp_SRAM_conv(i, j)
                  //val output_bpooled = Reg[TB](0.to[TB]); output_bpooled := tmp_SRAM_conv(i,j)
                  // if (L%2.to[Int] == 0.to[Int]) { output_bpooled := batchnorm + pooling }
                  val output_bpooled = List.tabulate(pool_size, pool_size) { (pool_i, pool_j) =>
                                         tmp_SRAM_conv(i*pool_size + pool_i, j*pool_size + pool_j) 
                                       }.flatten.reduceTree{(a,b) => max(a,b)}     

                  //do pooling/batch normalization when L is odd 
                  val actual_layer_output = mux(L%2.to[Int] == 0.to[Int], output_current, output_bpooled)

                  val ri = i 
                  val cj = j 
                  fc_layer_conv_bit(ri, cj, outD_i) = batch_norm(output_bpooled)
                }
              }
            //}
            // fc_layer_conv_bit(i,j,outD_p).to[TB] 
            Foreach(or by 1, oc by 1, rt by 1 par 2) { (i,j, outD_p) =>
              fc_layer_conv(1-L%2, i, j, outD_p) =fc_layer_conv_bit(i,j,outD_p).to[TB]  //catSeq(Seq.tabulate(64) { p => fc_layer_conv_bit(i,j, outD_p*64 + p) }).as[TB]
            }
        }


        Sequential.Foreach(BATCH_SIZE_USERDEF by 1) { batch_img =>

          /* FP layer */
          fp_conv3x3_norm(batch_img)

          /* Bin-Conv Layer */
          Sequential.Foreach(flayer until slayer) { L =>
            conv3x3_pool(batch_img, L)
          }

          /* FC Layer */
          val tmp_SRAM_fc     = SRAM[TB](2, fc_max_in_channels)
          val tmp_SRAM_fc_bit = SRAM[Bit](8192)

          val output_dim = SIZE/8 // = 4

          val fc_in_channels     = LUT[Int](3)( 8192/64, 1024/64, 1024/64 )
          val fc_out_channels    = LUT[Int](3)( 1024, 1024, 10 )

          val total_fc_input = 512/64
          Foreach(output_dim by 1, output_dim by 1, total_fc_input by 1 par output_dim) { (r, c, ch) =>
              tmp_SRAM_fc(0.to[I32], r*16 + c*output_dim + ch) = fc_layer_conv(1.to[I32], r, c, ch)
          }

          
          Foreach(0 until 3) { L =>
            val fc_in_channels_c  = fc_in_channels(L)
            val fc_out_channels_c = fc_out_channels(L)
            val L_inx = L%2
            

            val weight_SRAM_long = SRAM[TB](fc_max_in_channels * fc_max_out_channels)
            //val weight_SRAM = SRAM[TB](fc_max_out_channels, fc_max_in_channels);

            val max_fc_size = fc_out_channels_c * fc_in_channels_c //for this layer
            weight_SRAM_long load fc_weight_DRAM(L, 0::max_fc_size par 8)

            //Foreach(fc_out_channels_c by 1, fc_in_channels_c by 1){ (o,i) =>
            //  weight_SRAM(o,i) = weight_SRAM_long(o * fc_in_channels_c + i)
           // }

            Sequential.Foreach(fc_out_channels(L) by 1) { out_i =>
              // Load 1 row of weight matrix

              // XNOR + Popcount op in lieu of dot product
              val prod = Reduce(Reg[T](0.to[T]))(fc_in_channels_c par 8){ in_i =>
                              val xnor_tmp = ~(tmp_SRAM_fc(L_inx, in_i) ^ weight_SRAM_long(out_i * fc_in_channels_c + in_i))
                              popcount(Seq.tabulate(64){ p => xnor_tmp.bit(p)}).to[T] //popcount here 
                         }{_+_}
              tmp_SRAM_fc(1-L_inx, out_i) = batch_norm(prod.value).to[TB] 
            }
          }

          /* Save Results */
          Foreach(0 until 10) { l =>
            out_results_SRAM(batch_img, l) = tmp_SRAM_fc(1.to[I32], l).to[Bit]
          }
        }

        outDRAM := out_results_SRAM(0.to[I32],0.to[I32])  
    }


 //  val fp_gold = (0::SIZE, 0::SIZE) { (r,c) =>

 //    }

    /* Test First Layer */


    println(outDRAM.value)
  }
}