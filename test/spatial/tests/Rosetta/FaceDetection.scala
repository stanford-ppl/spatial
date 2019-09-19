package spatial.tests.Rosetta

import spatial.dsl._
import spatial.targets._
import utils.implicits._

@spatial class FaceDetection extends SpatialTest {

	type Pixel = UInt8
	type UInt  = UInt32 

	type Pixel2 = FixPt[FALSE, _24, _0]
	type Pixel3 = UInt32

	type Float = FixPt[FALSE, _12, _20]

	@struct case class Size(width : Int, height : Int)

	@struct case class Point(x : Int, y : Int)

	@struct case class Rect(x  : Int,  y : Int, w : Int, h : Int)


	val max_height	 	= 240 
	val max_width 		= 320
	
	val image_size 		= max_height * max_width 
	val image_maxgrey   = 255 

	val window_size 	= 25

	val total_stages 	= 25 
	val total_nodes 	= 2913

	val total_coordinates = total_nodes * 12 
	val total_weights 	  = total_nodes * 3

	val sq_size 		= 2 
	val window_size_double 	= 2*window_size 

	val size_th 			= 2913
	
	val file_string 	= "spatial-lang/apps/src/Rosetta/"

	/* ############ Math function helpers #################### */ 
	def doRound(this_value : Float) : Int = {
		(this_value + mux(this_value >= 0.0.to[Float], 0.5.to[Float], -0.5.to[Float])).to[Int]
	}

	def intSqrt(num 	: Int) : UInt = {
		val num_u = num.to[UInt]

		val a = Reg[UInt](0)
		//val b = Reg[UInt](0)
		val c = Reg[UInt](0)
		val value_int = Reg[UInt](0)

		a := 0.to[UInt]
		c := 0.to[UInt] 
		
		value_int := num_u 
		Sequential.Foreach(16 by 1) { _ =>
			val c_temp 		= (c.value << 2) + (value_int.value >> 30)
			val val_temp 	= value_int.value << 2
			val a_temp 		= a.value << 1
			val b_temp 		= a_temp << 1 | 1
			c 			:= mux(c_temp >= b_temp, c_temp - b_temp, c_temp) 
			a 			:= mux(c_temp >= b_temp, a_temp + 1, a_temp) 
		    value_int 	:= val_temp 
		}
		a.value.to[UInt]
 	}
	/* ###################  */
			
	
	def initialize_weights_and_alphas(alpha1_dram 	: DRAM1[Int], 
									  alpha2_dram   : DRAM1[Int], 
									  weights1_dram : DRAM1[Int], 
									  weights2_dram : DRAM1[Int], 
									  tree_thresh_dram : DRAM1[Int]) : Unit = {


		val alpha1_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/alpha1_array.csv", ",")
		val alpha2_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/alpha2_array.csv", ",")

		val alpha1_vec = Array.tabulate(size_th) { i => alpha1_dat.apply(i) }
		val alpha2_vec = Array.tabulate(size_th) { i => alpha2_dat.apply(i) }

		setMem(alpha1_dram, alpha1_vec)
		setMem(alpha2_dram, alpha2_vec)

		val weights1_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/weights_array1.csv", ",")
		val weights2_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/weights_array2.csv", ",")

		val weights1_vec = Array.tabulate(size_th) { i => weights1_dat.apply(i) }
		val weights2_vec = Array.tabulate(size_th) { i => weights2_dat.apply(i) }

		setMem(weights1_dram, weights1_vec)
		setMem(weights2_dram, weights2_vec)

		val tree_thresh_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/tree_thresh.csv", ",")
		val tree_thresh_vec = Array.tabulate(size_th) { i => tree_thresh_dat.apply(i)}

		setMem(tree_thresh_dram, tree_thresh_vec)

	}

	
	def initialize_image(image_dram: DRAM2[Pixel]) : Unit = {
		val image_dat = loadCSV2D[Pixel]("/home/jcamach2/" + file_string + "FaceDetection/image_face.dat", ",", "\n")

		val image_matrix = Matrix.tabulate(max_height, max_width) { (i,j) => image_dat.apply(i,j).to[Pixel] }
		setMem(image_dram, image_matrix)
	}

	
	def initialize_rectangle(rectangle_dram: DRAM1[Int], 
							  rect_id 	: scala.Int) : Unit = {

		val rect_file = rect_id match {
			  case 0 => "rectangle0.dat"
              case 1 => "rectangle1.dat"
              case 2 => "rectangle2.dat"
              case 3 => "rectangle3.dat"
              case 4 => "rectangle4.dat"
              case 5 => "rectangle5.dat"
              case 6 => "rectangle6.dat"
              case 7 => "rectangle7.dat"
              case 8 => "rectangle8.dat"
              case 9 => "rectangle9.dat"
              case 10 => "rectangle10.dat"
              case 11 => "rectangle11.dat"
              case _ => ""
            }

		val rectangle_dat = loadCSV1D[Int]("/home/jcamach2/" + file_string + "FaceDetection/lut_values/" + rect_file, ",")

		val rect_vec = Array.tabulate(size_th) { i => rectangle_dat.apply(i).to[Int] }
		setMem(rectangle_dram, rect_vec)
	}

	
	def check_results(result_x : Array[Int], 
					  result_y : Array[Int],
					  result_w : Array[Int],
					  result_h : Array[Int],
					  result_size : Int) : Unit = {

		val gold_model_faces_dat = loadCSV2D[Int]("/home/jcamach2/" + file_string + "FaceDetection/face_correct_result.dat", " ", "\n")

		val correct_num_faces = 43

		val gold_model_faces_x = Array.tabulate(correct_num_faces) { i => gold_model_faces_dat.apply(i,0).to[Int] }
		val gold_model_faces_y = Array.tabulate(correct_num_faces) { i => gold_model_faces_dat.apply(i,1).to[Int] }
		val gold_model_faces_w = Array.tabulate(correct_num_faces) { i => gold_model_faces_dat.apply(i,2).to[Int] }
		val gold_model_faces_h = Array.tabulate(correct_num_faces) { i => gold_model_faces_dat.apply(i,3).to[Int] }

		val did_pass = if (result_size != correct_num_faces) {
							false 
				   		} else {
				   			val pass_x = gold_model_faces_x.zip(result_x) {_ == _}.reduce(_ && _)
				   			val pass_y = gold_model_faces_y.zip(result_y) {_ == _}.reduce(_ && _)
				   			val pass_w = gold_model_faces_w.zip(result_w) {_ == _}.reduce(_ && _)
				   			val pass_h = gold_model_faces_h.zip(result_h) {_ == _}.reduce(_ && _)
				   			pass_x && pass_y && pass_w && pass_h
			       		}

		print("Pass? " + did_pass)
		println()
	}


	/* Accel module */
	
	def imageScaler(data 		: SRAM2[Pixel], 
					dest_height : Int, 
					dest_width  : Int, 
					img1_data 	: DRAM2[Pixel]) : Unit = {

		val img1_sram = SRAM[Pixel](max_width)

		val src_height = 	data.rows
		val src_width  = 	data.cols 

		val w1	= src_width
		val h1  = src_height
		val w2  = dest_width
		val h2  = dest_height

		val rat = 0.to[Int]

		val x_ratio = (((w1 << 16) / w2) + 1).to[Int]
		val y_ratio = (((h1 << 16) / h2) + 1).to[Int]

		val parFactor = 2 //scatter/gather support for 2D structures
		Foreach (max_height by 1) { i =>
			Foreach(max_width by 1 par parFactor) { j =>
				if (i < h2 && j < w2) {
					img1_sram(j)  = data( (i*y_ratio) >> 16, (j*x_ratio) >> 16 )
				}
			}
			img1_data(i, 0::max_width) store img1_sram
		}

	}


	
	def integralImageUpdates(imageLineBuffer 		: RegFile2[Pixel], 
							 imageWindowBuffer 		: RegFile2[Pixel2],
							 squareImageBuffer 		: RegFile2[Pixel2],
							 integralWindowBuffer  	: RegFile2[Pixel3],
							 squareIntegralBuffer  	: RegFile2[Pixel3],
							 img1_data_sram  		: SRAM1[Pixel], 
							 c 						: I32) : Unit = {

		//List.tabulate(window_size, window_size) { (u,v) =>
		Foreach(window_size by 1, window_size by 1) { (u,v) =>
			integralWindowBuffer(u,v) = (integralWindowBuffer(u,v).to[Int] + 
				(imageWindowBuffer(u, v + 1).to[Int] - imageWindowBuffer(u, 0).to[Int])).to[Pixel3]
		}

		squareIntegralBuffer(0,0) = (squareIntegralBuffer(0,0).to[Int]  + (squareImageBuffer(0,1).to[Int]							- squareImageBuffer(0,0).to[Int] )).to[Pixel3] 
		squareIntegralBuffer(0,1) = (squareIntegralBuffer(0,1).to[Int]  + (squareImageBuffer(0,window_size).to[Int]  				- squareImageBuffer(0,0).to[Int] )).to[Pixel3] 
		squareIntegralBuffer(1,0) = (squareIntegralBuffer(1,0).to[Int]  + (squareImageBuffer(window_size - 1,1).to[Int] 			- squareImageBuffer(window_size - 1,0).to[Int])).to[Pixel3] 
		squareIntegralBuffer(1,1) = (squareIntegralBuffer(1,1).to[Int]  + (squareImageBuffer(window_size - 1,window_size).to[Int]  	- squareImageBuffer(window_size - 1,0).to[Int])).to[Pixel3] 

		/* ideal to parallelize all operations */
		Foreach(window_size_double - 1 by 1, window_size by 1 par 4) { (u,v) =>
			imageWindowBuffer(v, u) = if (u + v != window_size_double - 1) 
										imageWindowBuffer(v, u + 1)
									  else 
										imageWindowBuffer(v, u + 1) + imageWindowBuffer(v - 1, u + 1)

			squareImageBuffer(v, u) = if (u + v != window_size_double - 1)
										squareImageBuffer(v, u + 1)
									  else 
										squareImageBuffer(v, u + 1) + squareImageBuffer(v - 1, u + 1)
		}

		//List.tabulate(window_size - 1) { v =>
		Foreach(window_size - 1 by 1){ v => 
			imageWindowBuffer(v, window_size_double - 1) = imageLineBuffer(window_size - 1 - v, c).to[Pixel2]
			squareImageBuffer(v, window_size_double - 1) = imageLineBuffer(window_size - 1 - v, c).to[Pixel2] * imageLineBuffer(window_size - 1 - v, c).to[Pixel2]
		}
		
		imageWindowBuffer(window_size - 1, window_size_double - 1) = img1_data_sram(c).to[Pixel2]
	    squareImageBuffer(window_size - 1, window_size_double - 1) = img1_data_sram(c).to[Pixel2] * img1_data_sram(c).to[Pixel2]
	    

	    imageLineBuffer(*, c) <<= img1_data_sram(c) /* shifts down line buffer */ 

	}


	def main(args: Array[String]): Void  = {

		/* Main function starts here */
		val result_size = 50

		/* Input */ 
		val image_input_dram	  =  DRAM[Pixel](max_height, max_width)

		val alpha1_dram 	= DRAM[Int](size_th)
		val alpha2_dram 	= DRAM[Int](size_th)
		val weights1_dram 	= DRAM[Int](size_th)
		val weights2_dram 	= DRAM[Int](size_th)
		val tree_thresh_dram = DRAM[Int](size_th)


		val rectangles_array0_dram  = DRAM[Int](size_th)
		val rectangles_array1_dram  = DRAM[Int](size_th)
		val rectangles_array2_dram  = DRAM[Int](size_th)
		val rectangles_array3_dram  = DRAM[Int](size_th)
		val rectangles_array4_dram  = DRAM[Int](size_th)
		val rectangles_array5_dram  = DRAM[Int](size_th)
		val rectangles_array6_dram  = DRAM[Int](size_th)
		val rectangles_array7_dram  = DRAM[Int](size_th)
		val rectangles_array8_dram  = DRAM[Int](size_th)
		val rectangles_array9_dram  = DRAM[Int](size_th)
		val rectangles_array10_dram = DRAM[Int](size_th)
		val rectangles_array11_dram = DRAM[Int](size_th)

		initialize_image(image_input_dram)

		initialize_weights_and_alphas(alpha1_dram, alpha2_dram, weights1_dram, weights2_dram, tree_thresh_dram)

		initialize_rectangle(rectangles_array0_dram, 0)
		initialize_rectangle(rectangles_array1_dram, 1)
		initialize_rectangle(rectangles_array2_dram, 2)
		initialize_rectangle(rectangles_array3_dram, 3)
		initialize_rectangle(rectangles_array4_dram, 4)
		initialize_rectangle(rectangles_array5_dram, 5)
		initialize_rectangle(rectangles_array6_dram, 6)
		initialize_rectangle(rectangles_array7_dram, 7)
		initialize_rectangle(rectangles_array8_dram, 8)
		initialize_rectangle(rectangles_array9_dram, 9)
		initialize_rectangle(rectangles_array10_dram, 10)
		initialize_rectangle(rectangles_array11_dram, 11)

		/* Output */
		val result_x_dram	= DRAM[Int](result_size)
		val result_y_dram	= DRAM[Int](result_size)
		val result_w_dram	= DRAM[Int](result_size)
		val result_h_dram	= DRAM[Int](result_size)

		val img1_dram 		= DRAM[Pixel](max_height, max_width)

		val final_result 	= ArgOut[Int]


		Accel {

			val result_x	= SRAM[Int](result_size)
			val result_y	= SRAM[Int](result_size)
			val result_w	= SRAM[Int](result_size)
			val result_h	= SRAM[Int](result_size)

			val tree_thresh_array  = SRAM[Int](size_th)
			val	alpha1_array 	 = SRAM[Int](size_th)
			val	alpha2_array 	 = SRAM[Int](size_th)
			val	weights_haar1    = SRAM[Int](size_th)
			val	weights_haar2    = SRAM[Int](size_th)

			val rectangles_array0  = SRAM[Int](size_th)
			val rectangles_array1  = SRAM[Int](size_th)
			val rectangles_array2  = SRAM[Int](size_th)
			val rectangles_array3  = SRAM[Int](size_th)
			val rectangles_array4  = SRAM[Int](size_th)
			val rectangles_array5  = SRAM[Int](size_th)
			val rectangles_array6  = SRAM[Int](size_th)
			val rectangles_array7  = SRAM[Int](size_th)
			val rectangles_array8  = SRAM[Int](size_th)
			val rectangles_array9  = SRAM[Int](size_th)
			val rectangles_array10 = SRAM[Int](size_th)
			val rectangles_array11 = SRAM[Int](size_th)

			/* Block Modules to implement the application */
		
			def faceDetect(data 	 : SRAM2[Pixel],
						   result_x  : SRAM1[Int],
						   result_y  : SRAM1[Int],
						   result_w  : SRAM1[Int],
						   result_h  : SRAM1[Int],
						   result_reg : Reg[Int]) : Unit = {

				val scaleFactor = 1.2.to[Float] /* Parameter to tune */
				val image_width = data.cols
				val image_height = data.rows

				val winSize0 	= Size(24.to[Int],24.to[Int])
				/* FSM states */
				val done = 1
				val not_done = 0

				val factor = Reg[Float](scaleFactor)
				factor := scaleFactor 

				val winSize = Reg[Size]

				FSM(not_done)(factor_state => factor_state != 1) { factor_state => 
					if (factor_state == not_done) {
						winSize := Size(doRound(winSize0.width.to[Float] * factor.value), doRound( winSize0.height.to[Float] * factor.value))
						val sz =   Size( (image_width.to[Float] / factor.value).to[Int], (image_height.to[Float] / factor.value).to[Int])

						imageScaler(data, sz.height, sz.width, img1_dram) 
						processImage(factor.value, sz.height, sz.width, result_x, result_y, result_w, result_h, result_reg, img1_dram, winSize.value) 

						factor := factor.value * scaleFactor 

					} else {}
				} { factor_state => mux(image_width/factor.value.to[Int] > window_size && image_height/factor.value.to[Int] > window_size, done, done)} 
			}

			
			def weakClassifier(stddev  	 	: UInt,
							   coord 		: RegFile1[Int], 
							   haar_counter : I32) : Int = {

				val weights_haar0 = -4096 // compiler error if I use -4096.to[Int]

				//val return_value = Reg[Int]

				val t = tree_thresh_array(haar_counter) * stddev.to[Int]
				val sum0 = (coord(0) - coord(1) - coord(2) + coord(3))   * weights_haar0
				val sum1 = (coord(4) - coord(5) - coord(6) + coord(7))   * weights_haar1(haar_counter)
				val sum2 = (coord(8) - coord(9) - coord(10) + coord(11)) * weights_haar2(haar_counter)
				val final_sum = sum0 + sum1 + sum2 

				mux(final_sum >= t, alpha2_array(haar_counter), alpha1_array(haar_counter))
				//return_value.value
			}

			
			def cascadeClassifier(sum 			: RegFile2[Pixel3], 
							   	  sqsum 		: RegFile2[Pixel3], 
							   	  p 			: Point) : Int = {

				val stages_lut 		  = LUT[Int](25)(9,16,27,32,52,53,62,72,83,91,99,115,127,135,136,137,159,155,169,196,197,181,199,211,200)
				val stages_thresh_lut = LUT[Int](25)(-1290,-1275,-1191,-1140,-1122,-1057,-1029,-994,-983,-933,-990,-951,-912,-947,-877,-899,-920,-868,-829,-821,-839,-849,-833,-862,-766)

				
				//Foreach(sum.rows by 1) { r => println(); Foreach(sum.cols by 1) { c => print(sum(r,c)); print(" ") } }
				//println()

				//val equRect 	= Reg[Rect]
				//equRect := Rect(0, 0, window_size, window_size) 


				val stddev_t 	=	sqsum(0, 0).to[Int] - sqsum(0, 1).to[Int] - sqsum(1, 0).to[Int] + sqsum(1, 1).to[Int]
				val mean  		=	sum(0, 0).to[Int] - sum(0, window_size - 1).to[Int] - sum(window_size - 1, 0).to[Int] + sum(window_size - 1, window_size - 1).to[Int]

				val stddev_t2	=	stddev_t * (window_size - 1) * (window_size - 1) - mean * mean
				val stddev 		=   mux(stddev_t2 > 0.to[Int], intSqrt(stddev_t2), 1.to[UInt])

				//val w_inx	= Reg[Index](0)
				//val r_inx 	= Reg[I32](0)

				val haar_counter = Reg[Int](0).buffer 
				val stage_sum 	 = Reg[Int](0).buffer

				haar_counter := 0.to[Int] 
				stage_sum := 0.to[Int]  

				 	/* states */
				val new_i 			= 0 
				val do_classifier 	= 1
				val done 			= 2 

				val coord = RegFile[Int](12).buffer
				val s     = Reg[Int](0) 

				val i = Reg[I32](0)
				i := 0.to[I32] 

				val bad_triangle = Reg[Boolean](false)
				bad_triangle := false

				val max_i = window_size.to[I32]
				FSM(do_classifier)(state => state != done) { state =>
				     if (state == do_classifier) {
						/* reset stage_sum and s to 0 for new stage */
						stage_sum := 0.to[Int]

						val max_j = stages_lut(i.value)
						Sequential.Foreach(max_j by 1) { j => 
							val x0 		= rectangles_array0(haar_counter.value)
							val width0 	= rectangles_array2(haar_counter.value)
							val y0 		= rectangles_array1(haar_counter.value)
							val height0 = rectangles_array3(haar_counter.value)

							val x1 		= rectangles_array4(haar_counter.value)
							val width1 	= rectangles_array6(haar_counter.value)
							val y1 		= rectangles_array5(haar_counter.value)
							val height1 = rectangles_array7(haar_counter.value)

							val x2 		= rectangles_array8(haar_counter.value)
							val width2 	= rectangles_array10(haar_counter.value)
							val y2 		= rectangles_array9(haar_counter.value)
							val height2 = rectangles_array11(haar_counter.value)

							val tr0		= Rect(x0, y0, width0, height0)
							val tr1		= Rect(x1, y1, width1, height1)
							val tr2		= Rect(x2, y2, width2, height2)

							coord(0) = sum(tr0.y, tr0.x).to[Int]  
							coord(1) = sum(tr0.y, tr0.x + tr0.w).to[Int] 
							coord(2) = sum(tr0.y + tr0.h,  tr0.x).to[Int] 
							coord(3) = sum(tr0.y + tr0.h, tr0.x + tr0.w).to[Int] 

							coord(4) = sum(tr1.y, tr1.x).to[Int]  
							coord(5) = sum(tr1.y, tr1.x + tr1.w).to[Int] 
							coord(6) = sum(tr1.y + tr1.h,  tr1.x).to[Int] 
							coord(7) = sum(tr1.y + tr1.h, tr1.x + tr1.w).to[Int]  

							val coords_not_zero = !(tr2.x == 0.to[Int] && tr2.w == 0.to[Int] && tr2.y == 0.to[Int] && tr2.h == 0.to[Int]) && tr2.w != 0 && tr2.h != 0
							coord(8)  = mux(coords_not_zero, sum(tr2.y, tr2.x).to[Int]					, 0.to[Int]) 
							coord(9)  = mux(coords_not_zero, sum(tr2.y, tr2.x + tr2.w).to[Int]			, 0.to[Int]) 
							coord(10) = mux(coords_not_zero, sum(tr2.y + tr2.h, tr2.x).to[Int]			, 0.to[Int]) 
							coord(11) = mux(coords_not_zero, sum(tr2.y + tr2.h, tr2.x + tr2.w).to[Int]	, 0.to[Int]) 

							/*println("coords: ")
							println(coord(0))
							println(coord(1))
							println(coord(2))
							println() */

							s := weakClassifier(stddev, coord, haar_counter.value.to[Int]) 

							stage_sum 	 := stage_sum + weakClassifier(stddev, coord, haar_counter.value.to[Int]) //s  
							haar_counter := haar_counter + 1 
							//println(haar_counter.value)
						}
								
						bad_triangle := stage_sum.value.to[Float] < 0.4.to[Float] * stages_thresh_lut(i.value).to[Float] 

						i := i + 1 
					} else {}
				} {
					state => mux(bad_triangle.value || i.value == max_i - 1.to[I32], done, do_classifier)
				}

				mux(!bad_triangle.value, 1.to[Int], -1.to[Int])
			}
			
			def processImage(factor 				: Float,
							 sum_row 				: Int, 
							 sum_col 				: Int, 
							 all_candidates_x		: SRAM1[Int],
							 all_candidates_y		: SRAM1[Int],
							 all_candidates_w		: SRAM1[Int],
							 all_candidates_h		: SRAM1[Int],
							 all_candidates_size	: Reg[Int],
							 img1_data 				: DRAM2[Pixel], 
							 win_size 				: Size) : Unit = {

				//val sum1_data 		= SRAM.buffer[Int](max_height, max_width)
				//val sqsum1_data 	 	= SRAM.buffer[Int](max_height, max_width)

				val imageLineBuffer 	  = RegFile[Pixel](window_size - 1, max_width).buffer /* 24 x 320 */
				val imageWindowBuffer     = RegFile[Pixel2](window_size, window_size_double).buffer
				val integralWindowBuffer  = RegFile[Pixel3](window_size, window_size).buffer

				val squareImageBuffer 	  = RegFile[Pixel2](window_size, window_size_double).buffer
				val squareIntegralBuffer  = RegFile[Pixel3](2, 2).buffer

				val img1_data_sram 		  = SRAM[Pixel](max_width)

				/* initialize integral buffers */
				Foreach(window_size by 1, window_size by 1 par 2) { (u,v) =>
					integralWindowBuffer(u,v) = 0.to[Pixel3]
				}
				squareIntegralBuffer(0,0) = 0.to[Pixel3]
				squareIntegralBuffer(0,1) = 0.to[Pixel3]
				squareIntegralBuffer(1,0) = 0.to[Pixel3]
				squareIntegralBuffer(1,1) = 0.to[Pixel3]

				Foreach(window_size by 1, window_size_double by 1 par 2) { (u,v) =>
					imageWindowBuffer(u,v) = 0.to[Pixel2]
					squareImageBuffer(u,v) = 0.to[Pixel2]
				}
				Foreach(window_size - 1 by 1, max_width by 1 par 2) { (u,v) =>
					imageLineBuffer(u,v) = 0.to[Pixel]
				}

				/* finished initiailizing all buffers */

				val r_index = Reg[Int](0).buffer
				val c_index = Reg[Int](0).buffer
				val element_counter = Reg[Int](0).buffer

				r_index := 0.to[Int]
				c_index := 0.to[Int]
				element_counter := 0.to[Int] 

				Foreach(sum_row by 1) { r =>
					//imageLineBuffer load img1_data(r, 0::max_width)
					img1_data_sram  load img1_data(r, 0::max_width par 4)
					Foreach(sum_col by 1) { c =>

						
						integralImageUpdates(imageLineBuffer, imageWindowBuffer, squareImageBuffer, integralWindowBuffer, squareIntegralBuffer, 
													img1_data_sram, c)

						/* start classifier task */
						val pixely_count = sum_row - window_size + 1
						val pixelx_count = sum_col - window_size + 1

						val x = c_index.value
						val y = r_index.value 
						val enough_elements_flushed_in = element_counter.value >= ((window_size - 1)*sum_col + window_size) + window_size - 1

						if (y < pixely_count && x < pixelx_count && enough_elements_flushed_in) { 
							val p = Reg[Point](Point(0,0))
							p := Point(x,y)

							//val result = Reg[Int](0)
							val result = cascadeClassifier(integralWindowBuffer, squareIntegralBuffer, p.value)
							if (result > 0.to[Int]) {
				      			val r = Rect( doRound(p.x.to[Float] * factor), doRound(p.y.to[Float] * factor), win_size.width, win_size.height )
				      			val size_i = all_candidates_size.value
				      			
				      			all_candidates_x(size_i) = r.x
			    	   			all_candidates_y(size_i) = r.y
			   					all_candidates_w(size_i) = r.w
			       				all_candidates_h(size_i) = r.h
				      			all_candidates_size := all_candidates_size + 1 
							}

						}

						if (enough_elements_flushed_in) {
							r_index := mux(c_index < sum_col-1, r_index    , r_index + 1)
							c_index := mux(c_index < sum_col-1, c_index + 1, 0)
						}

						element_counter := element_counter + 1
						

					}
					
				}

			}	


			/* Main Accel starts here */
			Parallel {
				tree_thresh_array load tree_thresh_dram 
				alpha1_array 	  load alpha1_dram 
				alpha2_array 	  load alpha2_dram
				weights_haar1 	  load weights1_dram 
				weights_haar2	  load weights2_dram
			}

			Parallel {
				rectangles_array0 	load rectangles_array0_dram 
				rectangles_array1 	load rectangles_array1_dram 
				rectangles_array2 	load rectangles_array2_dram 
				rectangles_array3 	load rectangles_array3_dram 
				rectangles_array4 	load rectangles_array4_dram 
				rectangles_array5 	load rectangles_array5_dram 
				rectangles_array6	load rectangles_array6_dram 
				rectangles_array7 	load rectangles_array7_dram 
				rectangles_array8 	load rectangles_array8_dram 
				rectangles_array9 	load rectangles_array9_dram 
				rectangles_array10 	load rectangles_array10_dram 
				rectangles_array11 	load rectangles_array11_dram 
			}

			val result_reg = Reg[Int](0)
			result_reg := 0 

			val image_sram  = SRAM[Pixel](max_height, max_width)

			/* Start Face Detection kernel */
			image_sram load image_input_dram // May want to do some gatter/scatter on img1_data 
		    faceDetect(image_sram, result_x, result_y, result_w, result_h, result_reg) 

			Parallel {
				result_x_dram store result_x 
				result_y_dram store result_y 
				result_w_dram store result_w 
				result_h_dram store result_h
			}
			final_result := result_reg

		}

		print(final_result.value)
		println()


		/* Test - print out values */
		val result_x_list = getMem(result_x_dram)
		val result_y_list = getMem(result_y_dram)
		val result_w_list = getMem(result_w_dram)
		val result_h_list = getMem(result_h_dram)

		for (i <- 0 until final_result.value) {
			print("Rect found: ")
			print(" x: " + result_x_list(i))
			print(" y: " + result_y_list(i))
			print(" w: " + result_w_list(i))
			print(" h: " + result_h_list(i))
			println()
		}

		check_results(result_x_list, result_y_list, result_w_list, result_h_list, final_result.value)
	}


}