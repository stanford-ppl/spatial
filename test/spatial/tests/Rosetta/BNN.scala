import spatial.dsl._
import spatial.targets._

@spatial class BNN extends SpatialTest {

  // -------------------------------------------------
  // Params
  // -------------------------------------------------

  val par_3x3_inner = 1
  val par_3x3_outer = 1
  val par_fc_inner = 1
  val par_fc_outer = 1
  
  val SIZE = 32 // row size 
  val B_3x3 = 1   // Block size for 3x3 convolution
  val BATCH_SIZE = 1

  // -------------------------------------------------
  // End of params
  // -------------------------------------------------  
  
  type T = FixPt[TRUE,_12,_20]
  type TB = UInt8

  val fc_max_in_channels  = 8192 // Note: This is aligned because of the 512
  val fc_max_out_channels = 1024

  def main(args: Array[String]): Void = {

//  val input 

//  val weights 
//	val fc_weights

  	val img_DRAM 		= DRAM[T](BATCH_SIZE, 3, SIZE, SIZE) // 3-channel 32 x 32 image 
  	val tmp1_DRAM   	= DRAM[T](BATCH_SIZE, 128, SIZE, SIZE)
  	val fp_weight_DRAM  = DRAM[TB](128, 3, 9)


  	val weight_DRAM 	= DRAM[TB](4224, 512, 9)
  	val fc_weight_DRAM	= DRAM[TB](4096 + 4096 + 1000, fc_max_in_channels)
  	val out_DRAM 		= DRAM[T](2, fc_max_in_channels)

  	val norm_comp = 0.to[T]

    // Initialize arrays - Note there is no bias in BNN's
    val input   = (0::BATCH_SIZE, 0::3, 0::SIZE, 0::SIZE) { (i,j,k,l) => 0.to[T] }    
    val fp_weights = (0::128, 0::3, 0::9) { (i,j,k) => 0.to[TB] } // 16 is 3x3 = 9, rounded up to the next alignment

    val fc_weights = (0::(4096 + 4096 + 1000), 0::fc_max_in_channels) { (i,j) => 0.to[TB] }
 
 	setMem(img_DRAM, input)  
 	setMem(fp_weight_DRAM, fp_weights)

 	setMem(fc_weight_DRAM, fc_weights)

  	Accel {

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

  			//Conv2D 
  			val B = 1
  			Foreach(out_channels by B) { outD_i =>	
  				val tmp_SRAM_conv = SRAM[T](B, max_nr, max_nc)

  				MemReduce(tmp_SRAM_conv)(in_channels by 1) { inD_i =>
  					// Load Kernel
  					val bin_weight_SRAM = SRAM[TB](B, 1, 9)
  					bin_weight_SRAM load fp_weight_DRAM(outD_i :: outD_i + B, inD_i :: inD_i + 1, 0 :: 9)

  					// Load a 2D feature map
		            val img2D = SRAM[T](max_nr, max_nc)
        		    img2D load img_DRAM(batch_img, inD_i, 0::nr, 0::max_nc) // This should be 0::nc
         
		         	// A block of 2D outputs
		            val result = SRAM[T](B, max_nr, max_nc)

		            Foreach(0 until nr, 0 until nc, 0 until B) { (r,c,b) =>
		              val row_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-r   )) // If I don't use .to[Int] here it has Spatial errors
		              val row_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nr-r))
		              val col_start = min(2.to[Int], max(0.to[Int], 1.to[Int]-c   ))
		              val col_end   = min(3.to[Int], max(1.to[Int], 1.to[Int]+nc-c))

		              /* Sign-Inversion in FP conv stage for BNN's (replaces multiply-add convolution) */
		              val window = Reduce(Reg[T](0.to[T]))(row_start until row_end, col_start until col_end){ (i,j) =>
		                val pix = img2D(i-1+r, j-1+c) 
		                mux( bin_weight_SRAM(outD_i,0,3*i + j) == 0.to[TB], pix, -pix)
		              }{_+_}

		              result(b, r, c) = window.value
		            }

		            result
  				}{_ + _} // Reduce across in channels


  				Foreach(B by 1) { b =>
  					tmp1_DRAM(batch_img, outD_i + b :: outD_i + b + 1, 0::or, 0::max_oc) store tmp_SRAM_conv
  				}

  				/*
  				val B_pool = mux(L & 1.to[Int] == 1.to[Int], B, 0.to[Int])
  				Foreach(B_pool by 1) { b =>
  					val tmp_SRAM_pool = SRAM[T](max_or, max_oc)
  					Foreach(or by 1, oc by 1) { (i,j) =>
  						val conv_value = tmp_SRAN_conv(b, i * pool_size, j * pool_size)
  						tmp_SRAM_pool(i,j) = mux(, 0.to[T], -1.to[T])
  					}


  				} */
  			}

  		}


  		/* Start of BNN accelerator */
  		Sequential.Foreach(BATCH_SIZE by 1) { batch_img => 

  			// FP-Conv
  			fp_conv3x3_norm(batch_img)

  			//Bin-Conv-Layer 
  			//Sequential.Foreach(0 until 6) { L =>
			//	conv3x3_pool(batch_img, L)  				
  			//}

  		}
  	}


	val tmp_bnn = getTensor4(tmp1_DRAM)
	val out_bnn = getMatrix(out_DRAM)
	printMatrix(out_bnn)


  }

}