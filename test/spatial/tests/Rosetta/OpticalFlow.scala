import spatial.dsl._
import spatial.targets._

@spatial class OpticalFlow extends SpatialTest {

  type Pixel     = FixPt[TRUE, _9, _23]
  type PixelLong = FixPt[TRUE, _9, _23] //55]

  type Frame = UInt8

  val window_inx = 5
  val buf_num = 7

  /* Par factors */
  val parLoad   = 4
  val parStore  = 4

  val max_height = 436 
  val max_width = 1024

  @struct case class IntAndIndex(value : Int, inx : I32)

  @struct case class Gradient(x : Pixel, y : Pixel, z : Pixel, padding : UInt32)

  @struct case class Velocity(x : Pixel, y : Pixel)

  @struct case class TriPixel(x : Pixel, y : Pixel, z : Pixel, padding : UInt32)

  @struct case class Tensor(t1  : Pixel, 
                t2  : Pixel,
                t3  : Pixel,
                t4  : Pixel,
                t5  : Pixel,
                t6  : Pixel, 
                padding : UInt64)

  @struct case class Outer(o1   : Pixel, 
               o2   : Pixel,
               o3   : Pixel,
               o4   : Pixel,
               o5   : Pixel,
               o6   : Pixel, 
               padding : UInt64)

  /**** Start Computation ****/
  def gradient_xy_compute(frame        :  DRAM2[Frame], 
                          gradient_x   :  FIFO[Pixel],
                          gradient_y   :  FIFO[Pixel]) : Unit = {

    val max_height = frame.rows 
    val max_width  = frame.cols 

   
    //val window_buffer = RegFile[Pixel](window_inx, window_inx) 
    val grad_weights  = LUT[Int](window_inx)(1.to[Int], -8.to[Int], 0.to[Int], 8.to[Int], -1.to[Int])
    val gw_max_inx    = grad_weights.length - 1

    val lb_frame      = LineBuffer[Frame](window_inx, max_width) //5 x cols 
 
    Foreach(max_height + 2 by 1) { row =>
      lb_frame load frame(row, 0::max_width par parLoad)

      Foreach(max_width + 2 by 1) { col =>

       // List.tabulate(window_inx) { i =>
        //  window_buffer(i, *) <<= (lb_frame(gw_max_inx - i, col).to[Pixel] / 255.0.to[Pixel])
       // }

        /* Note: window_inx = 5 */
        val col_t = col - 2
        val row_t = row - 2
        val do_set_to_0 = col_t < window_inx - 3 || col_t >= max_width - 2 || row_t < window_inx - 3 || row_t >= max_height - 2 

        val grad_x = List.tabulate(window_inx){ px =>
            val lb_frame_xi   = lb_frame(2.to[I32], (col - gw_max_inx) + px).to[Pixel] / 255.0.to[Pixel]
            val update_grad_x = lb_frame_xi * grad_weights(px).to[Pixel]
            mux(do_set_to_0, 0.to[Pixel], update_grad_x)
        }.reduceTree(_ + _)

        val grad_y = List.tabulate(window_inx){ px =>
            val lb_frame_yi   = lb_frame(px, col - 2).to[Pixel] / 255.0.to[Pixel]
            val update_grad_y = lb_frame_yi * grad_weights(gw_max_inx - px).to[Pixel]
            mux(do_set_to_0, 0.to[Pixel], update_grad_y)
        }.reduceTree(_ + _)

        if (col >= 2 && row >= 2) {
           gradient_x.enq(grad_x / 12.to[Pixel])
           gradient_y.enq(grad_y / 12.to[Pixel])
       }
    }

  }
}

   def gradient_z_compute(frame1 : DRAM2[Frame],
                          frame2 : DRAM2[Frame],
                          frame3 : DRAM2[Frame],
                          frame4 : DRAM2[Frame],
                          frame5 : DRAM2[Frame],
                          gradient_z : FIFO[Pixel]) : Unit = {

    val max_height = frame1.rows 
    val max_width = frame1.cols 

    val lb_frame1 = SRAM[Frame](max_width)
    val lb_frame2 = SRAM[Frame](max_width)
    val lb_frame4 = SRAM[Frame](max_width)
    val lb_frame5 = SRAM[Frame](max_width)

    val grad_weights = LUT[Pixel](window_inx)(1.0.to[Pixel], -8.0.to[Pixel], 0.0.to[Pixel], 8.0.to[Pixel], -1.0.to[Pixel])
    Foreach(max_height by 1) { h =>

      Parallel {
        lb_frame1 load frame1(h, 0::max_width par parLoad)
        lb_frame2 load frame2(h, 0::max_width par parLoad)
        lb_frame4 load frame4(h, 0::max_width par parLoad)
        lb_frame5 load frame5(h, 0::max_width par parLoad)
      }

      Foreach(max_width  by 1) { w =>
        val frame1_pixel = lb_frame1(w).to[Pixel] / 255.0.to[Pixel]
        val frame2_pixel = lb_frame2(w).to[Pixel] / 255.0.to[Pixel]
        val frame4_pixel = lb_frame4(w).to[Pixel] / 255.0.to[Pixel]
        val frame5_pixel = lb_frame5(w).to[Pixel] / 255.0.to[Pixel]

        val new_gradient_z_value = ( frame1_pixel * grad_weights(0) + frame2_pixel * grad_weights(1) + 
                                     frame4_pixel * grad_weights(3) + frame5_pixel * grad_weights(4) ) / 12.0.to[Pixel] 

        gradient_z.enq(new_gradient_z_value)
      }

    }
  
  }


  def gradient_weight_y(gradient_x  : FIFO[Pixel], 
                        gradient_y  : FIFO[Pixel], 
                        gradient_z  : FIFO[Pixel], 
                        filt_grad   : FIFO[Gradient]) : Unit = {

    val buf_limit     = buf_num/2 /* buf_limit = 3*/

    val gradient_zero = Gradient(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt32])

    val grad_filter = LUT[Pixel](buf_num)(0.0755.to[Pixel], 0.133.to[Pixel], 0.1869.to[Pixel], 
                                          0.2903.to[Pixel], 0.1869.to[Pixel], 0.133.to[Pixel], 
                                          0.0755.to[Pixel])

    //val filt_grad_lb   = SRAM[Gradient](max_width).buffer 
    val lb_frame_xyz   = LineBuffer[TriPixel](buf_num, max_width) /* 7 x width */

    Foreach(max_height + buf_limit by 1) { r =>

        /* shift down sram manually */
        if (r < max_height) {

          Foreach(max_width by 1) { _ =>
             lb_frame_xyz.enqAt(0.to[I32], TriPixel(gradient_x.deq(), gradient_y.deq(), gradient_z.deq(), 0.to[UInt32]) )
          }
        }

        /* gradient computation */
        Foreach(max_width by 1 par 1) {  c =>
            val accum_grad_y = List.tabulate(buf_num) { gx =>

               val update_x = lb_frame_xyz(gx, c).x * grad_filter(gx)
               val update_y = lb_frame_xyz(gx, c).y * grad_filter(gx)
               val update_z = lb_frame_xyz(gx, c).z * grad_filter(gx)

               val new_gradient = Gradient(update_x, update_y, update_z, 0.to[UInt32])
               mux( r >= buf_num - 1 && r < max_height, new_gradient, gradient_zero)   

            }.reduceTree{ (g1, g2) => Gradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z,0.to[UInt32]) }

           if (r >= buf_limit) {
              filt_grad.enq( accum_grad_y )
           }

        } 

       // if (r >= buf_limit) {
       //   filt_grad(r - buf_limit, 0::max_width par parStore) store filt_grad_lb
       // }
    }

  }


  def gradient_weight_x(y_filt      : FIFO[Gradient], 
                        filt_grad   : FIFO[Gradient]) : Unit = {

    val buf_limit = buf_num/2

    val gradient_zero = Gradient(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt32])
    val buf           = RegFile[Gradient](1, buf_num)

    val grad_filter = LUT[Pixel](buf_num)(0.0755.to[Pixel], 0.133.to[Pixel], 0.1869.to[Pixel], 
                        0.2903.to[Pixel], 0.1869.to[Pixel], 0.133.to[Pixel],
                        0.0755.to[Pixel])


    val y_frame      = SRAM[Gradient](max_width)

    Foreach(max_height by 1) { r =>
        
        Foreach(max_width by 1 par 1) { c => y_frame(c) = y_filt.deq() }

        Foreach(max_width + buf_limit by 1) {  c =>        
            val shift_up_new_gradient = mux(c < max_width, y_frame(c), gradient_zero)
            buf(0,*) <<= shift_up_new_gradient

            val accum_grad_x = List.tabulate(buf_num){ gx => {  /*buf_num = 7 */
                                  val update_x = buf(0,gx).x * grad_filter(gx)
                                  val update_y = buf(0,gx).y * grad_filter(gx)
                                  val update_z = buf(0,gx).z * grad_filter(gx)
                                  val new_gradient = Gradient(update_x, update_y, update_z,0.to[UInt32])
                                  mux( c >= buf_num - 1 && c < max_width, new_gradient, gradient_zero)
                                }
                              }.reduceTree { (g1, g2) => Gradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z, 0.to[UInt32]) }

            if  (c >= buf_limit) {
              filt_grad.enq( accum_grad_x )
            }                           
        }
    }

  }


  def outer_product(gradient      : FIFO[Gradient], 
                    outer_product : FIFO[Outer]) : Unit = {

    val gradient_lb  = SRAM[Gradient](max_width) 

    Foreach(max_height by 1) { r =>
      /* dequeue from gradient and save into lb */
      Foreach(max_width by 1){ w =>  gradient_lb(w) = gradient.deq() }

      Foreach(max_width by 1) { c =>
          val grad = gradient_lb(c)
          val out  = Outer(grad.x * grad.x,  
                           grad.y * grad.y, 
                           grad.z * grad.z,
                           grad.x * grad.y,  
                           grad.x * grad.z,  
                           grad.y * grad.z, 0.to[UInt64])
           outer_product.enq(out)
      }
    } 
  }



  def tensor_weight_y(outer    : FIFO[Outer], 
                      tensor_y : FIFO[Tensor]) = {


    val tensor_filter   = LUT[Pixel](3)(0.3243.to[Pixel], 0.3513.to[Pixel], 0.3243.to[Pixel])

    //val tensor_buffer  = SRAM[Tensor](max_width) 
    val outer_lb   = SRAM[Outer](tensor_filter.length, max_width)
    val buf_num    = tensor_filter.length

    Foreach(max_height + 1 by 1) { r =>
      if (r < max_height) {
         /* Foreach(buf_num - 1 by 1, max_width by 1 par 4) { (b,c) =>
            val buf_max_inx = buf_num - 1
            outer_lb(buf_max_inx - b, c) = outer_lb(buf_max_inx - 1 - b, c)
          }
          Foreach(max_width by 1 par 4) { c => 
            outer_lb(0, c) = outer.deq()
          } */ 
          Foreach(max_width by 1) { _ =>
            outer_lb.enqAt(0.to[I32], outer.deq() )
          }
        }
      Foreach(max_width by 1) { c =>
          val tmp_outer   =  Outer(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt64])  
          val zero_tensor =  Tensor(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt64])
          
          //val update_outer = mux(r < max_height, outer_buffer(r), tmp_outer)
          //buf(0,*) <<= update_outer 

          val acc_tensor = List.tabulate(3) { t =>
                              val current_grad = mux(r < max_height, outer_lb(t, c), tmp_outer)
                              val k = tensor_filter(t)
                              val new_tensor = Tensor(current_grad.o1 * k, current_grad.o2 * k, current_grad.o3 * k, current_grad.o4 * k,
                                                      current_grad.o5 * k, current_grad.o6 * k, 0.to[UInt64])
                              mux(r >= 2 && r < max_height, new_tensor, zero_tensor)
                            }.reduceTree{ (tensor1, tensor2) => Tensor(tensor1.t1 + tensor2.t1, tensor1.t2 + tensor2.t2, tensor1.t3 + tensor2.t3, 
                                                                       tensor1.t4 + tensor2.t4, tensor1.t5 + tensor2.t5, tensor1.t6 + tensor2.t6, 0.to[UInt64]) }
                  
          if (r >= 1) {
            tensor_y.enq(acc_tensor)
          }
        // tensor_buffer(c) = acc_tensor

       }
    }
  }


  def tensor_weight_x(tensor_y : FIFO[Tensor], 
                      tensor   : FIFO[Tensor]) = {


    val tensor_filter   = LUT[Pixel](3)(0.3243.to[Pixel], 0.3513.to[Pixel], 0.3243.to[Pixel])
    val buf   = RegFile[Tensor](1,tensor_filter.length)

    val tensor_y_buf  = SRAM[Tensor](max_width) 
   // val tensor_buf    = SRAM[Tensor](max_width)

    Foreach(max_height by 1) { r =>

      Foreach(max_width by 1 par 1) { c => tensor_y_buf(c) = tensor_y.deq() }

      Foreach(max_width + 1 by 1) { c =>
        val zero_tensor   = Tensor(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel], 0.to[UInt64])
        val update_tensor = mux(c < max_width, tensor_y_buf(c), zero_tensor)

        buf(0,*) <<= update_tensor

        val acc_tensor = List.tabulate(3) { t =>
                            val current_grad = buf(0,t)
                            val k = tensor_filter(t)
                            val new_tensor = Tensor(current_grad.t1 * k, current_grad.t2 * k, current_grad.t3 * k, current_grad.t4 * k,
                                                    current_grad.t5 * k, current_grad.t6 * k, 0.to[UInt64])
                            mux(c >= 2 && c < max_width, new_tensor, zero_tensor)
                        }.reduceTree{ (tensor1, tensor2) => Tensor(tensor1.t1 + tensor2.t1, tensor1.t2 + tensor2.t2, tensor1.t3 + tensor2.t3, 
                                                                   tensor1.t4 + tensor2.t4, tensor1.t5 + tensor2.t5, tensor1.t6 + tensor2.t6, 0.to[UInt64]) }
         
        if (c >= 1) {          
          tensor.enq (acc_tensor)
        }
      }
     // tensor(r, 0::max_width) store tensor_buf
    }

  }


  def compute_flow(tensors :   FIFO[Tensor], 
                   outputs_x : DRAM2[Pixel], 
                   outputs_y : DRAM2[Pixel]) = {

    val tensor_buffer = SRAM[Tensor](outputs_x.cols)
    val outputs_x_buf = SRAM[Pixel](outputs_x.cols)
    val outputs_y_buf = SRAM[Pixel](outputs_y.cols)

    /* outputs_x.rows/cols == outputs_y.rows/cols*/
    Foreach(outputs_x.rows by 1) { r => 
    
      Foreach(outputs_x.cols by 1) { c => tensor_buffer(c) = tensors.deq() }

      Foreach(outputs_x.cols by 1) { c =>
        /* Needs very low-precision FP ops. This is best handled with actual floating-point hardware support */
        val curr_tensor = tensor_buffer(c)

        val curr_tensor_t1 = curr_tensor.t1.to[PixelLong]
        val curr_tensor_t2 = curr_tensor.t2.to[PixelLong]
        val curr_tensor_t3 = curr_tensor.t3.to[PixelLong]
        val curr_tensor_t4 = curr_tensor.t4.to[PixelLong]
        val curr_tensor_t5 = curr_tensor.t5.to[PixelLong]
        val curr_tensor_t6 = curr_tensor.t6.to[PixelLong]

        val denom = curr_tensor_t1 * curr_tensor_t2 - curr_tensor_t4 * curr_tensor_t4 

        val vec_x = (curr_tensor_t6 * curr_tensor_t4 - curr_tensor_t5 * curr_tensor_t2).to[Float] / denom.to[Float]
        val vec_y = (curr_tensor_t5 * curr_tensor_t4 - curr_tensor_t6 * curr_tensor_t1).to[Float] / denom.to[Float]
        //print(curr_tensor)

        if (r >= 2 && r < outputs_x.rows - 2 &&  c  >= 2 && c < outputs_x.cols - 2) 
                {outputs_x_buf(c) = vec_x.to[Pixel]; outputs_y_buf(c) = vec_y.to[Pixel] } 
               else 
                {outputs_x_buf(c) = 0.to[Pixel]; outputs_y_buf(c) = 0.to[Pixel]} 
      }

      Parallel { 
        outputs_x(r, 0::outputs_x.cols) store outputs_x_buf
        outputs_y(r, 0::outputs_y.cols) store outputs_y_buf
      }

    } 

  }


  def initialize_frame(frame_dram : DRAM2[Frame], 
             frame_id : scala.Int) : Unit = {

    val use_sintel_alley = false 
    val dataset_name = if (use_sintel_alley) "sintel_alley/" else "current/"

    val frame_file = frame_id match {
              case 1 => "frame1.txt"
              case 2 => "frame2.txt"
              case 3 => "frame3.txt"
              case 4 => "frame4.txt"
              case 5 => "frame5.txt"
              case _ => ""
            }

    val actual_frame_file = "/home/jcamach2/spatial-lang/apps/src/Rosetta/OpticalFlow/datasets/" + dataset_name + frame_file

    val frame_csv = loadCSV2D[Frame](actual_frame_file, ",", "\n")

    val max_height = frame_dram.rows 
    val max_width = frame_dram.cols


    /* Set corresponding frame in DRAM */
    val frame_matrix = Matrix.tabulate(max_height, max_width){ (h, w) => {
                                        val frame_pixel = frame_csv.apply(h, w).to[Frame]
                                        frame_pixel
                                       }
              }

    setMem(frame_dram, frame_matrix)

  }

  def main(args: Array[String]): Void  = {

    /* Input */ 
    val frame1 = DRAM[Frame](max_height, max_width)
    val frame2 = DRAM[Frame](max_height, max_width)
    val frame3_a = DRAM[Frame](max_height, max_width)
    val frame3_b = DRAM[Frame](max_height, max_width)
    val frame4 = DRAM[Frame](max_height, max_width)
    val frame5 = DRAM[Frame](max_height, max_width)


    initialize_frame(frame1, 1)
    initialize_frame(frame2, 2)
    initialize_frame(frame3_a, 3)
    initialize_frame(frame3_b, 3)
    initialize_frame(frame4, 4)
    initialize_frame(frame5, 5)

    /* Output */
    val velocity_outputs_dram_x   =  DRAM[Pixel](max_height, max_width)
    val velocity_outputs_dram_y   =  DRAM[Pixel](max_height, max_width)

    /* Intermmediate values */

    val velocity_outputs_x = DRAM[Pixel](max_height, max_width)
    val velocity_outputs_y = DRAM[Pixel](max_height, max_width)


    Accel {
      val fifo_depth = max_width

      val gradient_x      = FIFO[Pixel](fifo_depth)
      val gradient_y      = FIFO[Pixel](fifo_depth)
      val gradient_z      = FIFO[Pixel](fifo_depth)

      val y_filtered          = FIFO[Gradient](fifo_depth)
      val grad_filtered       = FIFO[Gradient](fifo_depth)
      val curr_out_product    = FIFO[Outer](fifo_depth)
      val tensor_y            = FIFO[Tensor](fifo_depth)
      val tensor_final        = FIFO[Tensor](fifo_depth)

      Stream {  
          Parallel {
            gradient_xy_compute(frame3_a, gradient_x, gradient_y) 
            gradient_z_compute(frame1, frame2, frame3_b, frame4, frame5, gradient_z) 
          }
          gradient_weight_y(gradient_x, gradient_y, gradient_z, y_filtered) 
          gradient_weight_x(y_filtered, grad_filtered) 
          outer_product(grad_filtered, curr_out_product) 
          tensor_weight_y(curr_out_product, tensor_y) 
          tensor_weight_x(tensor_y, tensor_final) 
          compute_flow(tensor_final, velocity_outputs_dram_x, velocity_outputs_dram_y)
       }
          
    }

    val velocity_outputs_matrix_x = getMatrix(velocity_outputs_dram_x)
    val velocity_outputs_matrix_y = getMatrix(velocity_outputs_dram_y)

    def sq(this_number : Pixel) : Pixel = { this_number * this_number }

    val velocity_final_output =  (0::max_height, 0::max_width){(i,j) => 
                                    if ( sq(velocity_outputs_matrix_x(i,j)) + sq(velocity_outputs_matrix_y(i,j)) > 25.0.to[Pixel]) 
                                      Velocity(1e10.to[Pixel], 1e10.to[Pixel])
                                       else 
                                      Velocity(velocity_outputs_matrix_x(i,j), velocity_outputs_matrix_y(i,j)) }

                                    
    //val tensor_output = getMatrix(tensor_y)                                 
    val print_velocity_output = true 
    if (print_velocity_output) {
      for (i <- 0 until max_height) {
        println(i)
        for (j <- 0 until max_width) {
          print( velocity_outputs_matrix_x.apply(i,j) )
          print( " ")
          print( velocity_outputs_matrix_y.apply(i,j) )
          println()
        }
        println() 
      }
    } 
    println("pass?")
 
   /*
    val grad_output = getMatrix(debug_grad_x)                                    
    if (print_velocity_output) {
      for (i <- 0 until max_height) {
        for (j <- 0 until 20 + 0*max_width) {
          print( grad_output (i,j) ) //velocity_final_output.apply(i,j) )
          print(", ")
        }
        println() 
        println()
      } 
    } */




  }


}
