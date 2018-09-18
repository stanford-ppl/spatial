package spatial.tests.Rosetta

import spatial.dsl._
import spatial.targets._

@spatial class Rendering3D extends SpatialTest {
	override def runtimeArgs = "1 0"
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
				   pixels 			: SRAM1[Pixel],
				   frame_buffer		: SRAM2[UInt8]) : Unit = {

		Foreach(size_pixels by 1){ i =>
			frame_buffer(pixels(i).y.to[I32], pixels(i).x.to[I32]) = pixels(i).color
		}
	}

	def zculling(fragments 	: SRAM1[CandidatePixel],
				 size_fragment : Int,
				 pixels 	: SRAM1[Pixel],
				 z_buffer 	: SRAM2[UInt8]) : Int = {

		val pixel_cntr = Reg[Int](0) 
		Pipe { pixel_cntr := 0 }

		Foreach(size_fragment by 1) { p =>
			if (fragments(p).z < z_buffer(fragments(p).y.to[I32], fragments(p).x.to[I32])) {
				Pipe { pixels(pixel_cntr.value.to[I32]) = Pixel(fragments(p).x, fragments(p).y, fragments(p).color) }
				Pipe { pixel_cntr := pixel_cntr + 1 }
				z_buffer(fragments(p).y.to[I32], fragments(p).x.to[I32]) = fragments(p).z
			}

		}

		pixel_cntr.value
	}

	def rasterization2(flag			: Boolean,
					   max_min		: RegFile1[UInt8],
					   max_index	: Reg[Int], 
					   sample_triangle2D : triangle2D, 
					   fragment 	: SRAM1[CandidatePixel]) : Int = {

		def pixel_in_triangle(x : Int, y : Int, tri2D : triangle2D) : Boolean = {

			val pi0 = Reg[Int]
			val pi1 = Reg[Int]
			val pi2 = Reg[Int]

			Parallel {
				pi0 := (x - tri2D.x0.to[Int]) * (tri2D.y1.to[Int] - tri2D.y0.to[Int]) -
					   (y - tri2D.y0.to[Int]) * (tri2D.x1.to[Int] - tri2D.x0.to[Int])
				pi1 := (x - tri2D.x1.to[Int]) * (tri2D.y2.to[Int] - tri2D.y1.to[Int]) - 
					   (y - tri2D.y1.to[Int]) * (tri2D.x2.to[Int] - tri2D.x1.to[Int])
				pi2 := (x - tri2D.x2.to[Int]) * (tri2D.y0.to[Int] - tri2D.y2.to[Int]) -
					   (y - tri2D.y2.to[Int]) * (tri2D.x0.to[Int] - tri2D.x2.to[Int])
			}
			
			(pi0.value >= 0.to[Int] && pi1.value >= 0.to[Int] && pi2.value >= 0.to[Int])

		}

		val color = 100.to[UInt8]
		val i = Reg[Int](0.to[Int]) 
		Pipe { i := 0 }
		if ( (!flag) ) {
			Foreach(max_index.value by 1) { k =>
				val x = max_min(0).to[Int] + k.to[Int] % max_min(4).to[Int]
				val y = max_min(2).to[Int] + k.to[Int] / max_min(4).to[Int]


				val in_triangle = Reg[Boolean](false).buffer
				Pipe { in_triangle := pixel_in_triangle(x, y, sample_triangle2D) }

				val frag_x = x.to[UInt8]
				val frag_y = y.to[UInt8]
				val frag_z = sample_triangle2D.z
				val frag_color = color 

				if (in_triangle.value == true) {
					Pipe { fragment(i.value.to[I32]) = CandidatePixel(frag_x, frag_y, frag_z, frag_color) }
					Pipe { i := i + 1 }
				}
			
			}
		}
		mux(flag, 0.to[Int], i.value) /* if (flag) 0 else i */
	}

	/* calculate bounding box for 2D triangle */
	def rasterization1(sample_triangle2D : Reg[triangle2D], 
					   max_min			 : RegFile1[UInt8],
					   max_index		 : Reg[Int]) : Boolean = {

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
				Pipe { max_min(4) = max_x - min_x }
				Pipe { max_index := (max_x.to[Int] - min_x.to[Int]) * (max_y.to[Int] - min_y.to[Int]) }
			}
			//Pipe { max_min(4) = max_min(1) - max_min(0) }
			//Pipe { max_index := (max_min(1).to[Int] - max_min(0).to[Int]) * (max_min(3).to[Int] - max_min(2).to[Int]) }
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

		val input_reg = ArgIn[Int] // input value should be 3192 
		setArg(input_reg, args(0).to[Int])

		val input_num = input_reg.value

		val num_triangles = 3192 

		val run_on_board = false // args(1).to[Int] > 0.to[Int]  
		val input_file_name = if (!run_on_board) "/home/jcamach2/spatial-lang/apps/src/Rosetta/3D-Rendering/input_triangles.csv" else "/home/jcamach2/Rendering3D/input_triangles.csv"
		val output_file_name = if (!run_on_board) "/home/jcamach2/spatial-lang/apps/src/Rosetta/3D-Rendering/sw_output.csv" else "/home/jcamach2/Rendering3D/sw_output.csv"
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

		val vec_sram_len = num_triangles //24

		Accel {
			val angle = 0.to[Int]
	
			val z_buffer = SRAM[UInt8](img_y_size, img_x_size)
			val frame_buffer  = SRAM[UInt8](img_y_size, img_x_size)

			/* instantiate buffer */
			Foreach(img_y_size by 1, img_x_size by 1 par 24) { (i,j) =>
				z_buffer(i,j) = 255.to[UInt8]
				frame_buffer(i,j) = 0.to[UInt8]
			}
			
			Foreach(num_triangles by vec_sram_len) { i =>

				val load_len = min(vec_sram_len.to[Int], num_triangles - i)
				val triangle3D_vector_sram = SRAM[triangle3D](vec_sram_len)

				triangle3D_vector_sram load triangle3D_vector_dram(i :: i + load_len par 8) 
	
				Foreach(load_len by 1) { c =>

					val curr_triangle3D = triangle3D_vector_sram(c)
					val tri2D = Reg[triangle2D].buffer

					val max_min	= RegFile[UInt8](5)
					val max_index = Reg[Int](0)
						
					val pixels = SRAM[Pixel](500)
	
					val flag = Reg[Boolean](false)
					val size_fragment = Reg[Int](0.to[Int])
					val size_pixels = Reg[Int](0.to[Int])

					val fragment = SRAM[CandidatePixel](500)

					Pipe { projection(curr_triangle3D, tri2D, angle) }
					Pipe { flag := rasterization1(tri2D, max_min, max_index) }
					Pipe { size_fragment := rasterization2(flag.value, max_min, max_index, tri2D, fragment) }
					Pipe { size_pixels := zculling(fragment, size_fragment.value, pixels, z_buffer) }
					Pipe { coloringFB(size_pixels.value, pixels, frame_buffer) }

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

		val print_output = input_reg.value > 0.to[Int]
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
