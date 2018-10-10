import spatial.dsl._
import spatial.targets._

@spatial class OpticalFlow extends SpatialTest {

  type Pixel = FixPt[TRUE, _9, _23]

  type Frame = UInt8

  val window_inx = 5
  val buf_num = 7

  /* Par factors */
  val parLoad   = 1
  val parStore  = 1

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


  def gradient_xy_compute_tmp(frame       :  DRAM2[Frame], 
                          gradient_x  :  DRAM2[Pixel],
                          gradient_y  :  DRAM2[Pixel]) : Unit = {

    val max_height = frame.rows 
    val max_width  = frame.cols 

    val window_buffer = RegFile[Pixel](window_inx, window_inx)
    val grad_weights  = LUT[Int](window_inx)(1.to[Int], -8.to[Int], 0.to[Int], 8.to[Int], -1.to[Int])
    val gw_max_inx    = grad_weights.length - 1

    val lb_frame      = LineBuffer[Frame](window_inx, max_width) //5 x cols 
    val gradient_x_lb = SRAM[Pixel](max_width).buffer
    val gradient_y_lb = SRAM[Pixel](max_width).buffer

    Foreach(max_height + 2 by 1) { row =>
      lb_frame load frame(row, 0::max_width par parLoad)

      Foreach(max_width + 2 by 1) { col =>

        Foreach(window_inx by 1 par window_inx){i => window_buffer(i, *) <<= lb_frame(gw_max_inx - i, col).to[Pixel] / 255.0.to[Pixel]}
        /* Note: window_inx = 5 */
        val col_t = col - 2
        val row_t = row - 2
        val do_set_to_0 = col_t < window_inx - 3 || col_t > max_width - 2 || row_t < window_inx - 3 || row_t > max_height - 2 

        val grad_x = Reduce(Reg[Pixel](0.to[Pixel]))(window_inx by 1){ px =>
          val update_grad_x = window_buffer(2.to[I32], px) * grad_weights(gw_max_inx - px).to[Pixel]
          mux(do_set_to_0 /* col_t < window_inx - 3 || col_t > max_width - 2 */, 0.to[Pixel], update_grad_x / 12.to[Pixel])
        }{_ + _}
        val grad_y = Reduce(Reg[Pixel](0.to[Pixel]))(window_inx by 1){ px =>
          val update_grad_y = window_buffer(px, 2.to[I32]) * grad_weights(gw_max_inx - px).to[Pixel]
          mux(do_set_to_0 /* row_t < window_inx - 3 || row_t > max_height - 2 */, 0.to[Pixel], update_grad_y / 12.to[Pixel])
        }{_ + _}

        if (col >= 2) {
           gradient_x_lb(col - 2) = grad_x 
           gradient_y_lb(col - 2) = grad_y 
        }

      }

      if (row >= 2) {
         gradient_x(row - 2, 0::max_width par parStore) store gradient_x_lb
         gradient_y(row - 2, 0::max_width par parStore) store gradient_y_lb
      }
    }

  }


  def gradient_z_compute_tmp(frame1 : DRAM2[Frame],
                         frame2 : DRAM2[Frame],
                         frame3 : DRAM2[Frame],
                         frame4 : DRAM2[Frame],
                         frame5 : DRAM2[Frame],
                         gradient_z : DRAM2[Pixel]) : Unit = {

    val max_height = frame1.rows 
    val max_width = frame1.cols 

    val lb_frame1 = SRAM[Frame](max_width)
    val lb_frame2 = SRAM[Frame](max_width)
    val lb_frame4 = SRAM[Frame](max_width)
    val lb_frame5 = SRAM[Frame](max_width)

    val gradient_z_lb = SRAM[Pixel](max_width).buffer

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

        gradient_z_lb(w) = ( frame1_pixel * grad_weights(0) + frame2_pixel * grad_weights(1) + /*frame3_pixel * grad_weights(2) + */ 
                                    frame4_pixel * grad_weights(3) + frame5_pixel * grad_weights(4) ) / 12.0.to[Pixel] 
      }

      gradient_z(h, 0::max_width) store gradient_z_lb

    }
  
  }

  def gradient_weight_y_tmp(gradient_x  : DRAM2[Pixel], 
                        gradient_y  : DRAM2[Pixel], 
                        gradient_z  : DRAM2[Pixel], 
                        filt_grad   : DRAM2[Gradient]) : Unit = {

    
    val max_height = filt_grad.rows 
    val max_width  = filt_grad.cols

    val buf_limit     = buf_num/2 /* buf_limit = 3*/
   // val buf           = RegFile[Gradient](1, buf_num)
    val gradient_zero = Gradient(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt32])

    val grad_filter = LUT[Pixel](buf_num)(0.0755.to[Pixel], 0.133.to[Pixel], 0.1869.to[Pixel], 
                                          0.2903.to[Pixel], 0.1869.to[Pixel], 0.133.to[Pixel], 
                                          0.0755.to[Pixel])

    val filt_grad_lb = SRAM[Gradient](max_width).buffer /* 7 x width */
    val lb_frame_x   = LineBuffer[Pixel](buf_num, max_width)
    val lb_frame_y   = LineBuffer[Pixel](buf_num, max_width)
    val lb_frame_z   = LineBuffer[Pixel](buf_num, max_width)

    val max_lut_i = buf_num - 1
    val parFactor = buf_limit 

    Foreach(max_height + buf_limit by 1) { r =>
        Parallel {
          lb_frame_x load gradient_x(r, 0::max_width par parLoad)
          lb_frame_y load gradient_y(r, 0::max_width par parLoad)
          lb_frame_z load gradient_z(r, 0::max_width par parLoad)
        }
        Foreach(max_width by 1) {  c =>
            //  val shift_up_new_gradient = mux(r < max_height, Gradient(lb_frame_x(max_lut_i,c), lb_frame_y(max_lut_i,c), lb_frame_z(max_lut_i,c), 0.to[UInt32]), 
                                                         //     gradient_zero)
             // buf(0,*) <<= shift_up_new_gradient

              val accum_grad_y = Reduce(Reg[Gradient](gradient_zero))(buf_num by 1 par parFactor){ gx => { /* buf_num = 7 */
                                      val update_x = mux(r < max_height, lb_frame_x(gx, c) * grad_filter(gx), 0.to[Pixel])
                                      val update_y = mux(r < max_height, lb_frame_y(gx, c) * grad_filter(gx), 0.to[Pixel])
                                      val update_z = mux(r < max_height, lb_frame_z(gx, c) * grad_filter(gx), 0.to[Pixel]);
                                      val new_gradient = Gradient(update_x, update_y, update_z, 0.to[UInt32])
                                      mux( r >= buf_num - 1 && r < max_height, new_gradient, gradient_zero)
                                    }
                                  }{ (g1, g2) => Gradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z,0.to[UInt32]) }

              filt_grad_lb(c) = accum_grad_y
        }
        if (r >= buf_limit) {
          filt_grad(r - buf_limit, 0::max_width par parStore) store filt_grad_lb
        }
    }

  }

  /**** Start Computation ****/
  def gradient_xy_compute(frame       :  DRAM2[Frame], 
                          gradient_x  :  FIFO[Pixel],
                          gradient_y  :  FIFO[Pixel]) : Unit = {

    val max_height = frame.rows 
    val max_width  = frame.cols 

    val window_buffer = RegFile[Pixel](window_inx, window_inx)
    val grad_weights  = LUT[Int](window_inx)(1.to[Int], -8.to[Int], 0.to[Int], 8.to[Int], -1.to[Int])
    val gw_max_inx    = grad_weights.length - 1

    val lb_frame      = LineBuffer[Frame](window_inx, max_width) //5 x cols 

    Foreach(max_height + 2 by 1) { row =>
      lb_frame load frame(row, 0::max_width)

      Foreach(max_width + 2 by 1) { col =>

        Foreach(window_inx by 1 par window_inx){i => window_buffer(i, *) <<= lb_frame(gw_max_inx - i, col).to[Pixel] / 255.0.to[Pixel]}
        /* Note: window_inx = 5 */
        val col_t = col - 2
        val row_t = row - 2
        val do_set_to_0 = col_t < window_inx - 3 || col_t > max_width - 2 || row_t < window_inx - 3 || row_t > max_height - 2 

        val grad_x = Reduce(Reg[Pixel](0.to[Pixel]))(window_inx by 1){ px =>
          val update_grad_x = window_buffer(2.to[I32], px) * grad_weights(gw_max_inx - px).to[Pixel]
          mux(do_set_to_0, 0.to[Pixel], update_grad_x / 12.to[Pixel])
        }{_ + _}

        val grad_y = Reduce(Reg[Pixel](0.to[Pixel]))(window_inx by 1){ px =>
          val update_grad_y = window_buffer(px, 2.to[I32]) * grad_weights(gw_max_inx - px).to[Pixel]
          mux(do_set_to_0, 0.to[Pixel], update_grad_y / 12.to[Pixel])
        }{_ + _}

        if (col >= 2 && row >= 2) {
           gradient_x.enq(grad_x)
           gradient_y.enq(grad_y)
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
                        filt_grad   : DRAM2[Gradient]) : Unit = {

    
    val max_height = filt_grad.rows 
    val max_width  = filt_grad.cols

    val buf_limit     = buf_num/2 /* buf_limit = 3*/

    val gradient_zero = Gradient(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt32])

    val grad_filter = LUT[Pixel](buf_num)(0.0755.to[Pixel], 0.133.to[Pixel], 0.1869.to[Pixel], 
                                          0.2903.to[Pixel], 0.1869.to[Pixel], 0.133.to[Pixel], 
                                          0.0755.to[Pixel])

    val filt_grad_lb   = SRAM[Gradient](max_width).buffer /* 7 x width */
    val lb_frame_xyz   = SRAM[TriPixel](buf_num, max_width)

    val max_lut_i = buf_num - 1
    val parFactor = buf_limit 

    Foreach(max_height + buf_limit by 1) { r =>

        /* shift down sram fram manually */
        if (r < max_height) {
          Foreach(buf_num - 1 by 1, max_width by 1 par 8) { (b,c) =>
            lb_frame_xyz(buf_num + 1, c) = lb_frame_xyz(buf_num, c)
          }
          Foreach(max_width by 1) { c => 
            lb_frame_xyz(0, c) = TriPixel(gradient_x.deq(), gradient_y.deq(), gradient_z.deq(), 0.to[UInt32])
          }
        }
        /* gradient computation */
        Foreach(max_width by 1) {  c =>

              val accum_grad_y = Reduce(Reg[Gradient](gradient_zero))(buf_num by 1 par parFactor){ gx => { /* buf_num = 7 */
                                      val update_x = mux(r < max_height, lb_frame_xyz(gx, c).x * grad_filter(gx), 0.to[Pixel])
                                      val update_y = mux(r < max_height, lb_frame_xyz(gx, c).y * grad_filter(gx), 0.to[Pixel])
                                      val update_z = mux(r < max_height, lb_frame_xyz(gx, c).z * grad_filter(gx), 0.to[Pixel]);
                                      val new_gradient = Gradient(update_x, update_y, update_z, 0.to[UInt32])
                                      mux( r >= buf_num - 1 && r < max_height, new_gradient, gradient_zero)
                                    }
                                  }{ (g1, g2) => Gradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z,0.to[UInt32]) }

              filt_grad_lb(c) = accum_grad_y
             /* if (r >= buf_limit) {
                filt_grad.enq( accum_grad_y )
              }*/
        }

        if (r >= buf_limit) {
          filt_grad(r - buf_limit, 0::max_width) store filt_grad_lb
        }
    }

  }


  def gradient_weight_x(y_filt      : DRAM2[Gradient], 
                        filt_grad   : DRAM2[Gradient]) : Unit = {

    val max_height = y_filt.rows 
    val max_width = y_filt.cols 

    val buf_limit = buf_num/2

    val gradient_zero = Gradient(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt32])
    val buf           = RegFile[Gradient](1, buf_num)

    val grad_filter = LUT[Pixel](buf_num)(0.0755.to[Pixel], 0.133.to[Pixel], 0.1869.to[Pixel], 
                        0.2903.to[Pixel], 0.1869.to[Pixel], 0.133.to[Pixel],
                        0.0755.to[Pixel])


    val filt_grad_lb = SRAM[Gradient](max_width).buffer
    val y_frame      = SRAM[Gradient](max_width)

    Foreach(max_height by 1) { r =>
        y_frame load y_filt(r, 0::max_width)
        Foreach(max_width + buf_limit by 1) {  c =>        
            val shift_up_new_gradient = mux(c < max_width, y_frame(c), gradient_zero)
            buf(0,*) <<= shift_up_new_gradient

            val accum_grad_x = Reduce(Reg[Gradient](gradient_zero))(buf_num by 1){ gx => {/*buf_num = 7 */
                        val update_x = buf(0,gx).x * grad_filter(gx)
                        val update_y = buf(0,gx).y * grad_filter(gx)
                        val update_z = buf(0,gx).z * grad_filter(gx)
                        val new_gradient = Gradient(update_x, update_y, update_z,0.to[UInt32])
                        mux( c >= buf_num - 1 && c < max_width, new_gradient, gradient_zero)
                      }
                    }{ (g1, g2) => Gradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z, 0.to[UInt32]) }

            if  (c >= buf_limit) {
               filt_grad_lb(c - buf_limit) = accum_grad_x
            }               
        }
        filt_grad(r, 0::max_width) store filt_grad_lb
    }

  }


  def outer_product(gradient      : DRAM2[Gradient], 
                    outer_product : DRAM2[Outer]) : Unit = {

    val gradient_lb  = SRAM[Gradient](gradient.cols) 
    val outer_buffer = SRAM[Outer](outer_product.cols)

    Foreach(outer_product.rows by 1) { r =>
      gradient_lb load gradient(r, 0::outer_product.cols)
      Foreach(outer_product.cols by 1) { c =>
          val grad = gradient_lb(c)
          val out  = Outer(grad.x * grad.x,  
                           grad.y * grad.y, 
                           grad.z * grad.z,
                           grad.x * grad.y,  
                           grad.x * grad.z,  
                           grad.y * grad.z, 0.to[UInt64])
           outer_buffer(c) = out 
      }
      outer_product(r,0::outer_product.cols) store outer_buffer
    } 
  }



  def tensor_weight_y(outer    : DRAM2[Outer], 
                      tensor_y : DRAM2[Tensor]) = {


    val tensor_filter   = LUT[Pixel](3)(0.3243.to[Pixel], 0.3513.to[Pixel], 0.3243.to[Pixel])
    //val buf   = RegFile[Outer](1, tensor_filter.length)

    val tensor_buffer  = SRAM[Tensor](tensor_y.rows) 
    val outer_buffer   = LineBuffer[Outer](tensor_filter.length, outer.rows)

    Foreach(tensor_y.rows + 1 by 1) { r =>
      outer_buffer load outer(r, 0::tensor_y.cols)
      Foreach(tensor_y.cols by 1) { c =>
          val tmp_outer   =  Outer(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt64])  
          val zero_tensor =  Tensor(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[UInt64])
          
          //lval update_outer = mux(r < tensor_y.rows, outer_buffer(r), tmp_outer)
          //buf(0,*) <<= update_outer 

          val acc_tensor = Reduce(Reg[Tensor](zero_tensor))(tensor_filter.length by 1 par 3) { t =>
                              val current_grad = mux(r < tensor_y.rows, outer_buffer(t, c), tmp_outer)
                              val k = tensor_filter(t)
                              val new_tensor = Tensor(current_grad.o1 * k, current_grad.o2 * k, current_grad.o3 * k, current_grad.o4 * k,
                                                      current_grad.o5 * k, current_grad.o6 * k, 0.to[UInt64])
                              mux(r >= 2 && r < tensor_y.rows, new_tensor, zero_tensor)
                            }{ (tensor1, tensor2) => Tensor(tensor1.t1 + tensor2.t1, tensor1.t2 + tensor2.t2, tensor1.t3 + tensor2.t3, 
                                                            tensor1.t4 + tensor2.t4, tensor1.t5 + tensor2.t5, tensor1.t6 + tensor2.t6, 0.to[UInt64]) }
                  
          
          tensor_buffer(c) = acc_tensor
       }
       if (r >= 1) {
         tensor_y(r - 1, 0::tensor_y.cols) store tensor_buffer  
       }
    }
  }


  def tensor_weight_x(tensor_y : DRAM2[Tensor], 
                      tensor   : DRAM2[Tensor]) = {


    val tensor_filter   = LUT[Pixel](3)(0.3243.to[Pixel], 0.3513.to[Pixel], 0.3243.to[Pixel])
    val buf   = RegFile[Tensor](1,tensor_filter.length)

    val tensor_y_buf  = SRAM[Tensor](tensor_y.cols) 
    val tensor_buf    = SRAM[Tensor](tensor.cols)

    Foreach(tensor.rows by 1) { r =>
      tensor_y_buf load tensor_y(r, 0::tensor_y.cols)
      Foreach(tensor.cols  + 1 by 1) { c =>
        val zero_tensor   = Tensor(0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel],0.to[Pixel], 0.to[UInt64])
        val update_tensor = mux(c < tensor.cols, tensor_y_buf(c), zero_tensor)

        buf(0,*) <<= update_tensor

        val acc_tensor = Reduce(Reg[Tensor](zero_tensor))(tensor_filter.length by 1) { t =>
                            val current_grad = buf(0,t)
                            val k = tensor_filter(t)
                            val new_tensor = Tensor(current_grad.t1 * k, current_grad.t2 * k, current_grad.t3 * k, current_grad.t4 * k,
                                                    current_grad.t5 * k, current_grad.t6 * k, 0.to[UInt64])
                            mux(c >= 2 && c < tensor.cols, new_tensor, zero_tensor)
                        }{ (tensor1, tensor2) => Tensor(tensor1.t1 + tensor2.t1, tensor1.t2 + tensor2.t2, tensor1.t3 + tensor2.t3, 
                                                        tensor1.t4 + tensor2.t4, tensor1.t5 + tensor2.t5, tensor1.t6 + tensor2.t6, 0.to[UInt64]) }
         
        if (c >= 1) {
          tensor_buf(c - 1) = acc_tensor
        }
      }
      tensor(r, 0::tensor.cols) store tensor_buf
    }

  }


  def compute_flow(tensors :   DRAM2[Tensor], 
                   outputs_x : DRAM2[Pixel], 
                   outputs_y : DRAM2[Pixel]) = {

    val tensor_buffer = SRAM[Tensor](tensors.cols)
    val outputs_x_buf = SRAM[Pixel](outputs_x.cols)
    val outputs_y_buf = SRAM[Pixel](outputs_y.cols)

    Foreach(outputs_x.rows by 1) { r => 
      tensor_buffer load tensors(r, 0::tensors.cols)
      Foreach(outputs_x.cols by 1) { c =>
        val curr_tensor = tensor_buffer(c)
        val denom = curr_tensor.t1 * curr_tensor.t2 - curr_tensor.t4 * curr_tensor.t4

        val vec_x = (curr_tensor.t6 * curr_tensor.t4 - curr_tensor.t5 * curr_tensor.t2) /// denom
        val vec_y = (curr_tensor.t5 * curr_tensor.t4 - curr_tensor.t6 * curr_tensor.t1) // / denom
        if (r >= 2 && r < outputs_x.rows - 2 &&  c  >= 2 && c < outputs_x.cols - 2) 
                {outputs_x_buf(c) = vec_x; outputs_y_buf(c) = vec_y } 
               else 
                {outputs_x_buf(c) = 0.to[Pixel]; outputs_y_buf(c) = 0.to[Pixel]} 
      }

      Parallel { 
        outputs_x(r, 0::outputs_x.cols) store outputs_x_buf
        outputs_y(r, 0::outputs_y.cols) store outputs_y_buf
      }
     // if (r <= 20) { println() }
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

    val max_height = 436 
    val max_width = 1024

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
    val gradient_x_buf  = DRAM[Pixel](max_height, max_width)
    val gradient_y_buf  = DRAM[Pixel](max_height, max_width)
    val gradient_z_buf  = DRAM[Pixel](max_height, max_width)

    val y_filtered          = DRAM[Gradient](max_height, max_width)
    val grad_filtered       = DRAM[Gradient](max_height, max_width)
    val curr_out_product    = DRAM[Outer](max_height, max_width)
    val tensor_y            = DRAM[Tensor](max_height, max_width)
    val tensor_final        = DRAM[Tensor](max_height, max_width)

    val velocity_outputs_x = DRAM[Pixel](max_height, max_width)
    val velocity_outputs_y = DRAM[Pixel](max_height, max_width)


    Accel {
      val fifo_depth = 512
      //val gradient_x      = FIFO[Pixel](fifo_depth)
      //val gradient_y      = FIFO[Pixel](fifo_depth)
     //val gradient_z      = FIFO[Pixel](fifo_depth)

      val y_filtered_buf  = SRAM[Gradient](max_height, max_width)

      Sequential { 
          Parallel {
            gradient_xy_compute_tmp(frame3_a, gradient_x_buf, gradient_y_buf) 
            gradient_z_compute_tmp(frame1, frame2, frame3_b, frame4, frame5, gradient_z_buf) 
          }
         // gradient_weight_y_tmp(gradient_x_buf, gradient_y_buf, gradient_z_buf, y_filtered) 
          //gradient_weight_x(y_filtered, grad_filtered) 
          //outer_product(grad_filtered, curr_out_product) 
          //tensor_weight_y(curr_out_product, tensor_y) 
          //tensor_weight_x(tensor_y, tensor_final) 
          //compute_flow(tensor_final, velocity_outputs_dram_x, velocity_outputs_dram_y) 
      }
      //y_filtered store y_filtered_buf

    }

    val velocity_outputs_matrix_x = getMatrix(velocity_outputs_dram_x)
    val velocity_outputs_matrix_y = getMatrix(velocity_outputs_dram_y)

    def sq(this_number : Pixel) : Pixel = { this_number * this_number }

    val velocity_final_output =  (0::max_height, 0::max_width){(i,j) => 
                                    if ( sq(velocity_outputs_matrix_x(i,j)) + sq(velocity_outputs_matrix_y(i,j)) > 25.0.to[Pixel]) 
                                      Velocity(1e10.to[Pixel], 1e10.to[Pixel])
                                       else 
                                      Velocity(velocity_outputs_matrix_x(i,j), velocity_outputs_matrix_y(i,j)) }


    val filt_grad_output = getMatrix(gradient_z_buf)                                    
    val print_velocity_output = true
    if (print_velocity_output) {
      for (i <- 0 until max_height) {
        for (j <- 0 until 20 + 0*max_width) {
          print( filt_grad_output (i,j) ) //velocity_final_output.apply(i,j) )
          print(",")
        }
        println() 
      }
    }
    println("pass?")

    val y_grad_output = getMatrix(gradient_y_buf)                                    
    if (print_velocity_output) {
      for (i <- 0 until max_height) {
        for (j <- 0 until 20 + 0*max_width) {
          print( y_grad_output (i,j) ) //velocity_final_output.apply(i,j) )
          print(",")
        }
        println() 
      }
    }




  }


}