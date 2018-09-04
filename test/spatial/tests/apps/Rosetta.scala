package spatial.tests.Rosetta

import spatial.dsl._
import spatial.targets._

@spatial class Rendering3D extends SpatialTest {
	override def runtimeArgs = "0 3192 0"
	//override val target = Zynq
	// struct size of power of 2
	@struct case class triangle3D(x0 : UInt8, y0 : UInt8, z0 : UInt8, 
								  x1 : UInt8, y1 : UInt8, z1 : UInt8,
								  x2 : UInt8, y2 : UInt8, z2 : UInt8, 
								  padding1 : UInt32, padding2 : UInt16, padding3 : UInt8)

	@struct case class triangle2D(x0 : UInt8, y0 : UInt8,
								  x1 : UInt8, y1 : UInt8,
								  x2 : UInt8, y2 : UInt8, 
								  z  : UInt8)

	@struct case class CandidatePixel(x : UInt8, y : UInt8, z : UInt8, color : UInt8)

	@struct case class Pixel(x : UInt8, y : UInt8, color : UInt8)

	def coloringFB(size_pixels 		: Int,
				   pixels 			: FIFO[Pixel],
				   frame_buffer		: SRAM2[UInt8]) : Unit = {

		//println(" " + size_pixels)
		Foreach(size_pixels by 1){ i =>
			val curr_pixel = pixels.deq()
			frame_buffer(curr_pixel.y.to[I32], curr_pixel.x.to[I32]) = curr_pixel.color
		}
	}

	def zculling(fragments 	: FIFO[CandidatePixel],
				 size_fragment : Int,
				 pixels 	: FIFO[Pixel],
				 z_buffer 	: SRAM2[UInt8]) : Int = {

		val pixel_reg = Reg[Int](0)
		Pipe { pixel_reg := 0.to[Int] }

		Foreach(size_fragment by 1) { p =>
			val curr_fragment = fragments.deq() //(p)
			if (curr_fragment.z < z_buffer(curr_fragment.y.to[I32], curr_fragment.x.to[I32])) {
				pixels.enq(Pixel(curr_fragment.x, curr_fragment.y, curr_fragment.color))
				z_buffer(curr_fragment.y.to[I32], curr_fragment.x.to[I32]) = curr_fragment.z
				pixel_reg := pixel_reg + 1.to[Int]
			}
		}

		//pixels.numel.to[Int]
		pixel_reg.value 
	}

	def rasterization2(flag			: Boolean,
					   max_min		: RegFile1[UInt8],
					   xmax_index	: Reg[Int], 
					   ymax_index	: Reg[Int], 
					   sample_triangle2D : triangle2D, 
					   fragments 	: FIFO[CandidatePixel]) : Int = {

		def pixel_in_triangle(x : Int16, y : Int16, tri2D : triangle2D) : Boolean = {

			val pi0 =  (x - tri2D.x0.to[Int16]) * (tri2D.y1.to[Int16] - tri2D.y0.to[Int16]) -
					   (y - tri2D.y0.to[Int16]) * (tri2D.x1.to[Int16] - tri2D.x0.to[Int16])
			val pi1 =  (x - tri2D.x1.to[Int16]) * (tri2D.y2.to[Int16] - tri2D.y1.to[Int16]) - 
					   (y - tri2D.y1.to[Int16]) * (tri2D.x2.to[Int16] - tri2D.x1.to[Int16])
			val pi2 =  (x - tri2D.x2.to[Int16]) * (tri2D.y0.to[Int16] - tri2D.y2.to[Int16]) -
					   (y - tri2D.y2.to[Int16]) * (tri2D.x0.to[Int16] - tri2D.x2.to[Int16])
			
			(pi0 >= 0.to[Int16] && pi1 >= 0.to[Int16] && pi2 >= 0.to[Int16])

		}

		val color = 100.to[UInt8]
		val x_max = xmax_index.value //(max_min(1).to[I32] - max_min(0).to[I32]).to[I32]
		val y_max = ymax_index.value //(max_min(3).to[I32] - max_min(2).to[I32]).to[I32]

		val frag_reg = Reg[Int](0)
		Pipe { frag_reg := 0.to[Int] }
		if ( (!flag) ) { 
		   	Foreach(x_max by 1, y_max by 1 par 1){ (x_t, y_t) =>
				val x = max_min(0).to[Int16] + x_t.to[Int16]
				val y = max_min(2).to[Int16] + y_t.to[Int16]

				val in_triangle = pixel_in_triangle(x, y, sample_triangle2D) 

				val frag_x = x.to[UInt8]
				val frag_y = y.to[UInt8]
				val frag_z = sample_triangle2D.z
				val frag_color = color 

				if (in_triangle == true) {
					fragments.enq( CandidatePixel(frag_x, frag_y, frag_z, frag_color) ) 
					frag_reg := frag_reg +  1.to[Int]  
				}
			} /*
			Foreach(max_index.value.to[I32] by 1 par 4) { k =>
			 	val x = max_min(0).to[Int] + k.to[Int] % max_min(4).to[Int]
			  	val y = max_min(2).to[Int] + k.to[Int] / max_min(4).to[Int]

			  	val in_triangle = pixel_in_triangle(x, y, sample_triangle2D) 

				val frag_x = x.to[UInt8]
				val frag_y = y.to[UInt8]
			   	val frag_z = sample_triangle2D.z
				val frag_color = color 
	     	//	if  (in_triangle == true) {
    			Pipe { fragments.enq( CandidatePixel(frag_x, frag_y, frag_z, frag_color), in_triangle == true)	}									
		    //    }
		    } */
		}
		frag_reg.value 
		//mux(flag, 0.to[Int], fragments.numel.to[Int]) /* if (flag) 0 else i */
	}

	/* calculate bounding box for 2D triangle */
	def rasterization1(sample_triangle2D : Reg[triangle2D], 
					   max_min			 : RegFile1[UInt8],
					   xmax_index		 : Reg[Int], 
					   ymax_index 		 : Reg[Int]) : Boolean = {

		def check_clockwise(tri2D : triangle2D) : Int = {
			(tri2D.x2.to[Int] - tri2D.x0.to[Int]) * (tri2D.y1.to[Int] - tri2D.y0.to[Int]) - (tri2D.y2.to[Int] - tri2D.y0.to[Int]) * (tri2D.x1.to[Int] - tri2D.x0.to[Int])
		}

		def find_min(a : UInt8, b : UInt8, c : UInt8) : UInt8 = {
			mux(a < c, mux(a < b, a , b), mux(c < b, c, b))
		}

		def find_max(a : UInt8, b : UInt8, c : UInt8) : UInt8 = {
			mux(a > c, mux(a > b, a , b), mux(c > b, c, b))
		}

		val this_check = Reg[Int](0)
		Pipe { this_check := check_clockwise(sample_triangle2D.value) }

		/* correspomds to the clockwise_vertices function */
		if (this_check.value != 0.to[Int]) {

			val check_negative = this_check.value < 0.to[Int]
			val new_x0 = mux(check_negative, sample_triangle2D.x1, sample_triangle2D.x0)
			val new_y0 = mux(check_negative, sample_triangle2D.y1, sample_triangle2D.y0)
			val new_x1 = mux(check_negative, sample_triangle2D.x0, sample_triangle2D.x1)
			val new_y1 = mux(check_negative, sample_triangle2D.y0, sample_triangle2D.y1)

			Pipe { sample_triangle2D := triangle2D(new_x0, new_y0, new_x1, new_y1, 
										  		   sample_triangle2D.x2, sample_triangle2D.y2,
										    	   sample_triangle2D.z) } 
			
			val min_x =  find_min(sample_triangle2D.x0, sample_triangle2D.x1, sample_triangle2D.x2)
			val max_x =  find_max(sample_triangle2D.x0, sample_triangle2D.x1, sample_triangle2D.x2)
			val min_y =  find_min(sample_triangle2D.y0, sample_triangle2D.y1, sample_triangle2D.y2)
			val max_y =  find_max(sample_triangle2D.y0, sample_triangle2D.y1, sample_triangle2D.y2)

			Parallel { 
				Pipe { max_min(0) = min_x }
				Pipe { max_min(1) = max_x }
				Pipe { max_min(2) = min_y }
				Pipe { max_min(3) = max_y }
				Pipe { ymax_index := (max_y - min_y).to[Int] } //Pipe { max_min(4) = max_x - min_x }
				Pipe { xmax_index := (max_x - min_x).to[Int] }
			}
		}

		(this_check.value == 0.to[Int])
	}

	def projection(sample_triangle3D : triangle3D, 
				   proj_triangle2D	 : Reg[triangle2D], 
				   angle			 : Int) : Unit = {

		val x0 = Reg[UInt8]
		val y0 = Reg[UInt8]

		val x1 = Reg[UInt8]
		val y1 = Reg[UInt8]

		val x2 = Reg[UInt8]
		val y2 = Reg[UInt8]

		val z = Reg[UInt8]

		Parallel {

			x0 := mux(angle == 0, sample_triangle3D.x0, 
						mux(angle == 1, sample_triangle3D.x0, sample_triangle3D.z0))
			y0 := mux(angle == 0, sample_triangle3D.y0, 	
						mux(angle == 1, sample_triangle3D.z0, sample_triangle3D.y0))

			x1 := mux(angle == 0, sample_triangle3D.x1, 
						mux(angle == 1, sample_triangle3D.x1, sample_triangle3D.z1))
			y1 := mux(angle == 0, sample_triangle3D.y1, 	
						mux(angle == 1, sample_triangle3D.z1, sample_triangle3D.y1))

			x2 := mux(angle == 0, sample_triangle3D.x2, 
			    		 mux(angle == 1, sample_triangle3D.x2, sample_triangle3D.z2))
			y2 := mux(angle == 0, sample_triangle3D.y2, 	
						 mux(angle == 1, sample_triangle3D.z2, sample_triangle3D.y2)) 

			val f1 = sample_triangle3D.z0 / 3.to[UInt8] + sample_triangle3D.z1 / 3.to[UInt8] + sample_triangle3D.z2 / 3.to[UInt8]
			val f2 = sample_triangle3D.y0 / 3.to[UInt8] + sample_triangle3D.y1 / 3.to[UInt8] + sample_triangle3D.y2 / 3.to[UInt8]
			val f3 = sample_triangle3D.x0 / 3.to[UInt8] + sample_triangle3D.x1 / 3.to[UInt8] + sample_triangle3D.x2 / 3.to[UInt8]

			 z :=  mux(angle == 0, f1, mux(angle == 1, f2, f3)) 
		}

		Pipe { proj_triangle2D := triangle2D(x0.value, y0.value, x1.value, y1.value, x2.value, y2.value, z.value) }
	}

	def main(args: Array[String]): Void = {

		type T = FixPt[FALSE, _8, _0]

		val img_y_size = 256
		val img_x_size = 256

		val num_triangles = 3192 
		val last_triangle =  args(1).to[Int]

		val tri_start  = args(0).to[Int]

		val run_on_board = args(2).to[Int] > 0.to[Int]  
		val input_file_name = if (run_on_board) "/home/jcamach2/Rendering3D/input_triangles.csv" else s"$DATA/rosetta/3drendering_input_triangles.csv"
		val output_file_name = if (run_on_board) "/home/jcamach2/Rendering3D/sw_output.csv" else s"$DATA/rosetta/3drendering_sw_output.csv"
		val input_trianges_csv = loadCSV2D[T](input_file_name, ",", "\n")

		val output_image = DRAM[UInt8](img_y_size, img_x_size)
 
		/* Set all triangle3D structures in DRAM */
		val triangle3D_vector_host = Array.tabulate(num_triangles){ inx => {
																		val i = inx 
																		val x0 = input_trianges_csv.apply(i,0).to[UInt8]
																		val y0 = input_trianges_csv.apply(i,1).to[UInt8]
																		val z0 = input_trianges_csv.apply(i,2).to[UInt8]
																		val x1 = input_trianges_csv.apply(i,3).to[UInt8]
																		val y1 = input_trianges_csv.apply(i,4).to[UInt8]
																		val z1 = input_trianges_csv.apply(i,5).to[UInt8]
																		val x2 = input_trianges_csv.apply(i,6).to[UInt8]
																		val y2 = input_trianges_csv.apply(i,7).to[UInt8]
																		val z2 = input_trianges_csv.apply(i,8).to[UInt8]
																		val newTriangle = triangle3D(x0,y0,z0,x1,y1,z1,x2,y2,z2, 0.to[UInt32], 0.to[UInt16], 0.to[UInt8])
																		newTriangle
																	}
																  }
	
	

		val triangle3D_vector_dram = DRAM[triangle3D](num_triangles)
		setMem(triangle3D_vector_dram, triangle3D_vector_host)

		val host_z_buffer = DRAM[UInt8](img_y_size, img_x_size)
		val host_frame_buffer = DRAM[UInt8](img_y_size, img_x_size)

		val vec_sram_len = num_triangles

		Accel {
			val angle = 0.to[Int]
	
			val z_buffer = SRAM[UInt8](img_y_size, img_x_size)
			val frame_buffer  = SRAM[UInt8](img_y_size, img_x_size)

			/* instantiate buffer */
			Foreach(img_y_size by 1, img_x_size by 1 par 16) { (i,j) =>
				z_buffer(i,j) = 255.to[UInt8]
				frame_buffer(i,j) = 0.to[UInt8]
			}
			
			val do_triangles = last_triangle - tri_start
			Foreach(do_triangles by vec_sram_len) { i =>

				val load_len = min(vec_sram_len.to[Int], do_triangles - i)
				val triangle3D_vector_sram = SRAM[triangle3D](vec_sram_len)

				triangle3D_vector_sram load triangle3D_vector_dram(i :: i + vec_sram_len par 4) 
	
				Foreach(load_len by 1) { c =>

					val curr_triangle3D = triangle3D_vector_sram(c + tri_start)
					val tri2D = Reg[triangle2D].buffer

					val max_min	= RegFile[UInt8](5)
					val xmax_index = Reg[Int](0)
					val ymax_index = Reg[Int](0)

					val pixels = FIFO[Pixel](500)
	
					val flag = Reg[Boolean](false)
					val size_fragment = Reg[Int](0.to[Int])
					val size_pixels = Reg[Int](0.to[Int])

					val fragment = FIFO[CandidatePixel](500)

					'proj.Pipe { projection(curr_triangle3D, tri2D, angle) }
					'rast1.Pipe { flag := rasterization1(tri2D, max_min, xmax_index, ymax_index) } 
					'rast2.Pipe { size_fragment := rasterization2(flag.value, max_min, xmax_index, ymax_index, tri2D, fragment) }
					'zcull.Pipe { size_pixels := zculling(fragment, size_fragment.value, pixels, z_buffer) }
					'color.Pipe { coloringFB(size_pixels.value, pixels, frame_buffer) }

				}

			}

			Parallel { 
				//host_z_buffer store z_buffer 
				host_frame_buffer store frame_buffer
			}
		}

		//val z_buffer_screen = getMatrix(host_z_buffer)
		val frame_screen = getMatrix(host_frame_buffer)
	
		val print_frame = false
		if (print_frame) {      
			printMatrix(frame_screen)
		}

		/* output image file that's created by the Rosetta Software implementation - use for testing */
		val sw_original_output = loadCSV2D[T](output_file_name, ",", "\n")

		val print_just_in_case = false
		if (print_just_in_case) { /* this is for making sure that I'm loading the file correctly */
			for (i <- 0 until img_y_size) {
				for (j <- 0 until img_x_size) {
					print( sw_original_output.apply(i,j) )
				}
				println() 
			}
		}

		val frame_output = (0 :: img_y_size,  0 :: img_x_size) { (i,j) => if (frame_screen(i,j) > 0) 1.to[T] else 0.to[T] }
		val gold_output = (0 :: img_y_size,  0 :: img_x_size) { (i,j) => if (sw_original_output.apply(img_y_size - 1- i,j) > 0) 1.to[T] else 0.to[T] }

		val print_output = true
		if (print_output) { /* this is for making sure that I'm loading the file correctly */
			for (i <- 0 until img_y_size) {
				for (j <- 0 until img_x_size) {
					print( frame_output(img_y_size - 1 - i,j) )
				}
				println() 
			}
		}

		val cksum = frame_output.zip(gold_output) { _ == _ }.reduce( _ && _ )
		println("Pass? " + cksum)

	}
}


@spatial class DigitRecognition extends SpatialTest {

    type LabelType	 		= UInt8

	@struct case class IntAndIndex(value : Int, inx : I32)

	@struct case class LabelAndIndex(dist : Int, label : LabelType, inx : I32)
    
    @struct case class DigitType(d1 : UInt64, d2 : UInt64, d3 : UInt64, d4 : UInt64)

	@struct case class DigitType1(d1 : UInt64, d2 : UInt64)

    @struct case class DigitType2(d3 : UInt64, d4 : UInt64)


	val digit_length 		= 196
	val digit_field_num 	= 4

	val simulate_file_string 	= "spatial-lang/apps/src/Rosetta/"
	val empty_str = ""


	val num_training = 18000
	val class_size = 1800 /* number of digits in training set that belong to each class */
	val num_test = 2000


	/* Parameters to tune */
	val k_const 				= 4 /* Number of nearest neighbors to handle */
	val par_factor  			= 4



	def network_sort(knn_set 		: RegFile2[Int], 
					 label_set 		: RegFile2[LabelType],
					 p : I32) : Unit = {
		/* Odd-Even network sort on knn_set in-place */
		val num_elems = k_const /* should be size of each knn_set */
		Foreach(num_elems by 1) { i =>
			val start_i = mux( (i & 1.to[I32]) == 1.to[I32], 0.to[I32], 1.to[I32] )
			
			val oe_par_factor = num_elems >> 1  // might not work if regfile size is even 
			Foreach(start_i until num_elems - 1 by 2 par oe_par_factor) { k =>
				val value_1 = knn_set(p, k)
				val value_2 = knn_set(p, k + 1)
				val smaller_value = mux(value_1 <  value_2, value_1, value_2)
				val bigger_value  = mux(value_1 >= value_2, value_1, value_2)

				val label_1 = label_set(p, k)
				val label_2 = label_set(p, k + 1)
				val first_label  = mux(value_1 <  value_2, label_1, label_2)
				val second_label = mux(value_1 >= value_2, label_1, label_2)

				Parallel {
					knn_set(p, k) 	  = smaller_value 
					knn_set(p, k + 1) = bigger_value

					label_set(p, k)		 = first_label
					label_set(p, k + 1)  = second_label
				}
			}
		}
	}

	
	def merge_sorted_lists(par_knn_set 		: RegFile2[Int], 
						   par_label_set	: RegFile2[LabelType],
						   sorted_label_set	: RegFile1[LabelType]) : Unit = {


		val partition_index_list = RegFile[I32](par_factor)
		Foreach(par_factor by 1 par par_factor) { p => partition_index_list(p) = 0.to[Int] }

		Sequential.Foreach(k_const by 1) { k => //doesn't work without Sequential 
			val insert_this_label =	Reduce(Reg[LabelAndIndex])(par_factor by 1 par par_factor) { p =>
										val check_at_index = partition_index_list(p)
										val curr_index 	   = k_const + check_at_index

										val current_label = par_label_set(p, curr_index)
										val current_dist  = par_knn_set(p, curr_index)

										LabelAndIndex(current_dist, current_label, p)
									}{ (a, b) => mux(a.dist < b.dist, a, b) }
			Parallel {
				sorted_label_set(k) = insert_this_label.label 
				partition_index_list(insert_this_label.inx) = partition_index_list(insert_this_label.inx) + 1.to[I32]
			}
		}

	} 


	def hamming_distance(x1 : 	DigitType1, x2 : DigitType2 ) : Int = {

	
		// simplest implementation for counting 1-bits in bitstring
		val sum_bits_tmp = RegFile[Int](4)
		Parallel {  
			val bits_d1 = List.tabulate(64){ i =>
							val d1 = x1.d1 
							d1.bit(i).to[Int]
						  }.reduce(_ + _)
			sum_bits_tmp(0) = bits_d1.to[Int]

			val bits_d2 =  List.tabulate(64) { i =>
							val d2 = x1.d2 
							d2.bit(i).to[Int] 
						  }.reduce(_ + _)
			sum_bits_tmp(1) = bits_d2 

			val bits_d3 =  List.tabulate(64) { i =>
							val d3 = x2.d3 
							d3.bit(i).to[Int] 
						  }.reduce(_ + _)
			sum_bits_tmp(2) = bits_d3 
			
			val bits_d4 =  List.tabulate(64){ i =>
							val d4 = x2.d4
							d4.bit(i).to[Int] 
						  }.reduce(_ + _)
			sum_bits_tmp(3) = bits_d4 
		}

		sum_bits_tmp(0) + sum_bits_tmp(1) + sum_bits_tmp(2) + sum_bits_tmp(3)
	}

	def update_knn(test_inst1 	: DigitType1, 
				   test_inst2 	: DigitType2,	
				   train_inst1 	: DigitType1, 
				   train_inst2 	: DigitType2,
				   min_dists	: RegFile2[Int],
				   label_list   : RegFile2[LabelType],
				   p 			: I32,
				   train_label  : LabelType) : Unit = {

		val digit_xored1 = DigitType1(test_inst1.d1 ^ train_inst1.d1, test_inst1.d2 ^ train_inst1.d2)
		val digit_xored2 = DigitType2(test_inst2.d3 ^ train_inst2.d3, test_inst2.d4 ^ train_inst2.d4)

		val max_dist = Reg[IntAndIndex](IntAndIndex(0.to[Int], 0.to[I32]))
		max_dist.reset

		val dist = Reg[Int](0)
		Parallel { 
			Pipe { dist := hamming_distance(digit_xored1, digit_xored2) }

			Reduce(max_dist)(k_const by 1) { k =>
				IntAndIndex(min_dists(p, k), k)
			} {(dist1,dist2) => mux(dist1.value > dist2.value, dist1, dist2) }
		}	

		if (dist.value < max_dist.value.value) {
			Parallel { 
				min_dists(p, max_dist.value.inx) = dist.value  
				label_list(p, max_dist.value.inx) = train_label
			}
		}
		
	}

	def initialize(min_dists	: RegFile2[Int],
				   label_list	: RegFile2[LabelType],
				   vote_list	: RegFile1[Int]) = {
		val vote_len = vote_list.length /* should always be 10 */

		Parallel { 
			Foreach(par_factor by 1, k_const by 1 par k_const) { (p,k) => 
				min_dists(p,k) = 256.to[Int]
				label_list(p, k) = 0.to[LabelType]
			}
			Foreach(vote_len by 1) { v =>
				vote_list(v) = 0.to[Int]
			}
		}
	}



	def knn_vote(knn_set  	: RegFile2[Int],
				 label_list : RegFile2[LabelType],
				 vote_list	: RegFile1[Int]) : LabelType = {

		/* Apply merge-sort first. Each row of knn_set should already be sorted */
		val sorted_label_list = RegFile[LabelType](k_const)
		merge_sorted_lists(knn_set, label_list, sorted_label_list)

		Foreach(sorted_label_list.length by 1){ i =>
			vote_list( sorted_label_list(i).to[I32] ) = vote_list( sorted_label_list(i).to[I32] ) + 1
		}

		val best_label = Reg[IntAndIndex](IntAndIndex(0.to[Int], 0.to[I32]))
		best_label.reset

		Reduce(best_label)(vote_list.length by 1) { j =>
			IntAndIndex(vote_list(j), j)
		} {(l1,l2) => mux(l1.value > l2.value, l1, l2) }

		best_label.inx.to[LabelType]
	}

	def initialize_train_data(training_set_dram_1 : DRAM1[DigitType1], 
							  training_set_dram_2 : DRAM1[DigitType2], 
							  label_set_dram : DRAM1[LabelType], 
							  file_string : String) = {
		/* Rosetta sw version assumes unsigned long long => 64 bits. */

		val training0_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_0.dat", ",", "\n")
		val training1_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_1.dat", ",", "\n")
		val training2_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_2.dat", ",", "\n")
		val training3_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_3.dat", ",", "\n")
		val training4_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_4.dat", ",", "\n")	
		val training5_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_5.dat", ",", "\n")	
		val training6_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_6.dat", ",", "\n")	
		val training7_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_7.dat", ",", "\n")	
		val training8_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_8.dat", ",", "\n")	
		val training9_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/training_set_9.dat", ",", "\n")	

		val training_list = List(training0_dat, training1_dat, training2_dat, training3_dat, training4_dat, training5_dat, 
								 training6_dat, training7_dat, training8_dat, training9_dat)

		val entire_training_vec1	=	training_list.map( training_dat => Array.tabulate(class_size) { i =>
																	 		val sample1 = training_dat.apply(i, 0).to[UInt64]
																			val sample2 = training_dat.apply(i, 1).to[UInt64]
																			DigitType1(sample1, sample2)
																		} 
													 ).reduce(_ ++ _)

		val entire_training_vec2	=	training_list.map( training_dat => Array.tabulate(class_size) { i =>
																			val sample3 = training_dat.apply(i, 2).to[UInt64]
																			val sample4 = training_dat.apply(i, 3).to[UInt64]
																			DigitType2(sample3, sample4)
																		} 
													 ).reduce(_ ++ _)

		val print_vec = false  
		if (print_vec) { /* this is for making sure that I'm loading the file correctly */
			for (i <- 0 until class_size) {
				println(entire_training_vec1(i).d1)
				println(entire_training_vec1(i).d2)
			}
		}

		setMem(training_set_dram_1, entire_training_vec1)
		setMem(training_set_dram_2, entire_training_vec2)

		val label_vec = Array.tabulate(training_set_dram_1.length){ i => (i.to[Int] / class_size).to[LabelType] }
		setMem(label_set_dram, label_vec)
	}


	def initialize_test_data(test_set_dram_1 : DRAM1[DigitType1], 
							 test_set_dram_2 : DRAM1[DigitType2], 
							 file_string 	 : String) = {
		val test_dat = loadCSV2D[UInt64]("/home/jcamach2/" + file_string + "DigitRecognition/196data/test_set.dat", ",", "\n")

		val test_actual_vector1 = Array.tabulate(test_set_dram_1.length) { i =>
									val sample1 = test_dat.apply(i, 0).to[UInt64]
									val sample2 = test_dat.apply(i, 1).to[UInt64]
									DigitType1(sample1, sample2)
								}

		val test_actual_vector2 = Array.tabulate(test_set_dram_2.length) { i =>
									val sample3 = test_dat.apply(i, 2).to[UInt64]
									val sample4 = test_dat.apply(i, 3).to[UInt64]
									DigitType2(sample3, sample4)
								}

		setMem(test_set_dram_1, test_actual_vector1)	
		setMem(test_set_dram_2, test_actual_vector2)
	}


	def main(args: Array[String]): Void = {


		val run_on_board = (args(0).to[Int] > 0.to[Int]).as[Boolean]
		val file_string = if (run_on_board) empty_str else simulate_file_string 

		/* input */
		val training_set_dram_1 =  DRAM[DigitType1](num_training)	
		val training_set_dram_2 =  DRAM[DigitType2](num_training)

		val label_set_dram 	  =  DRAM[LabelType](num_training)

		val test_set_dram_1	  =	 DRAM[DigitType1](num_test)
		val test_set_dram_2	  =	 DRAM[DigitType2](num_test)

		/* output */
		val results_dram	  =  DRAM[LabelType](num_test)

		val num_test_local_len 		= 20 // num_test /* for on chip memory */
		val num_train_local_len 	= num_training

		initialize_train_data(training_set_dram_1, training_set_dram_2, label_set_dram, file_string)
		initialize_test_data(test_set_dram_1, test_set_dram_2, file_string)		

		val expected_results = loadCSV1D[LabelType]("/home/jcamach2/" + file_string + "DigitRecognition/196data/expected.dat", "\n")
 
		val test_dram = DRAM[Int](k_const)
		Accel {

			val train_set_local1 	= SRAM[DigitType1](num_train_local_len)
			val train_set_local2 	= SRAM[DigitType2](num_train_local_len)

			val label_set_local 	= SRAM[LabelType](num_train_local_len)

			val test_set_local1 	= SRAM[DigitType1](num_test_local_len)
			val test_set_local2 	= SRAM[DigitType2](num_test_local_len)

			/* ####### Debugging Sorting Functions  #############
			val test_sort = RegFile[Int](par_factor * k_const)
			val test_label = RegFile[LabelType](par_factor * k_const)
			val test_result = RegFile[LabelType](k_const)
			val test_sram = SRAM[Int](k_const)

			Foreach(0 until par_factor * k_const) { i =>
				test_sort(i) =  i.to[Int] + 2.to[Int]
				test_label(i) = i.to[LabelType]
			}
			test_sort(4) = 0.to[Int]

			Pipe { merge_sorted_lists(test_sort, test_label, test_result) }

			Foreach(0 until k_const) { i =>
				test_sram(i) = test_result(i).to[Int]
			}

			test_dram store test_sram 
			*/
		
			Parallel { 
				train_set_local1 load training_set_dram_1 //for now do this
				train_set_local2 load training_set_dram_2

				label_set_local load label_set_dram
			}

			Foreach(num_test by num_test_local_len){ test_factor =>

				Parallel { 
					test_set_local1 load test_set_dram_1(test_factor :: test_factor + num_test_local_len par 4)
					test_set_local2 load test_set_dram_2(test_factor :: test_factor + num_test_local_len par 4)
				}

				val results_local 	= SRAM[LabelType](num_test_local_len)
				Sequential.Foreach(num_test_local_len by 1) { test_inx =>

					val vote_list		= 	RegFile[Int](10).buffer

					/* initialize KNN */
					val knn_tmp_large_set = RegFile[Int](par_factor, k_const) // when parallelizing, size will need to be k_const * par_factor
					val label_list_tmp 	= 	RegFile[LabelType](par_factor, k_const)

					Pipe { initialize(knn_tmp_large_set, label_list_tmp, vote_list) }

					/* Training Loop */
					val train_set_par_num = num_train_local_len / par_factor

					val test_local1 = test_set_local1(test_inx)
					val test_local2 = test_set_local2(test_inx)

					Sequential.Foreach(par_factor by 1 par par_factor) { p => 
						Foreach(train_set_par_num by 1 par 1) { train_inx =>
							val curr_train_inx = p * train_set_par_num + train_inx
							Pipe { update_knn(test_local1, test_local2,
											  train_set_local1(curr_train_inx), train_set_local2(curr_train_inx),
											  knn_tmp_large_set, label_list_tmp, p, label_set_local(curr_train_inx)) }
						}
						Pipe { network_sort(knn_tmp_large_set, label_list_tmp, p) }
					}
					/* Do KNN */
					Pipe { results_local(test_inx) = knn_vote(knn_tmp_large_set, label_list_tmp, vote_list) }

				}

				results_dram(test_factor :: test_factor + num_test_local_len) store results_local 
			} 

		}

		val result_digits = getMem(results_dram)
    
		/* Test */
		val cksum =	result_digits.zip(expected_results){ (d1,d2) => if (d1 == d2) 1.0 else 0.0 }.reduce{ _ + _ }
		print(cksum)
		println(" out of " + num_test + " correct.")
	}


}
