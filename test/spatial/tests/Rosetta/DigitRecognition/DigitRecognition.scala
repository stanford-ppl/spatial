package spatial.tests.Rosetta

import spatial.dsl._
import spatial.targets._


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

/*
	def hamming_distance(x1 : 	DigitType1, x2 : DigitType2 ) : Int = {

		// simplest implementation for counting 1-bits in bitstring
		val sum_bits_tmp = RegFile[Int](4)
		Parallel {  
			val bits_d1 = List.tabulate(64){i => {val d1 = x1.d1 
												  d1(i).as[Int] } }
			Pipe { sum_bits_tmp(0) = Math.sumTree(bits_d1) }

			val bits_d2 = List.tabulate(64){i => {val d2 = x1.d2
												  d2(i).as[Int] } }
			Pipe { sum_bits_tmp(1) = Math.sumTree(bits_d2) }

			val bits_d3 = List.tabulate(64){i => {val d3 = x2.d3
												  d3(i).as[Int] } }
			Pipe { sum_bits_tmp(2) = Math.sumTree(bits_d3) }

			val bits_d4 = List.tabulate(64){i => {val d4 = x2.d4
												  d4(i).as[Int] } }
			Pipe { sum_bits_tmp(3) = Math.sumTree(bits_d4) }
		}

		sum_bits_tmp(0) + sum_bits_tmp(1) + sum_bits_tmp(2) + sum_bits_tmp(3)
	}


	def update_knn(test_inst1 	: DigitType1, 
				   test_inst2 	: DigitType2,	
				   train_inst1 	: DigitType1, 
				   train_inst2 	: DigitType2,
				   min_dists	: RegFile1[Int],
				   label_list   : RegFile1[LabelType],
				   train_label  : LabelType) : Unit = {

		val k_const = min_dists.length
		val digit_xored1 = DigitType1(test_inst1.d1 ^ train_inst1.d1, test_inst1.d2 ^ train_inst1.d2)
		val digit_xored2 = DigitType2(test_inst2.d3 ^ train_inst2.d3, test_inst2.d4 ^ train_inst2.d4)

		val max_dist = Reg[IntAndIndex](IntAndIndex(0.to[Int], 0.to[I32]))
		max_dist.reset

		val dist = Reg[Int](0)
		Parallel { 
			Pipe { dist := hamming_distance(digit_xored1, digit_xored2) }

			Reduce(max_dist)(k_const by 1) { k =>
				IntAndIndex(min_dists(k), k)
			} {(dist1,dist2) => mux(dist1.value > dist2.value, dist1, dist2) }
		}	

		if (dist.value < max_dist.value.value) {
			Parallel { 
				min_dists(max_dist.value.inx) = dist.value  
				label_list(max_dist.value.inx) = train_label
			}
		}
		
	}
*/
	def initialize(min_dists	: RegFile1[Int],
				   label_list	: RegFile1[LabelType],
				   vote_list	: RegFile1[Int]) = {
		val k_const = min_dists.length 
		val vote_len = vote_list.length /* should always be 10 */

		Parallel { 
			Foreach(k_const by 1 par k_const) { k =>
				min_dists(k) = 256.to[Int]
				label_list(k) = 0.to[LabelType]
			}
			Foreach(vote_len by 1 par vote_len){i =>
				vote_list(i) = 0.to[Int]
			}
		}
	}


	def network_sort(knn_set 		: RegFile1[Int]) : Unit = {
		/* Odd-Even network sort on knn_set in-place */
		val num_elems = knn_set.length 
		Foreach(num_elems by 1) { i =>
			val start_i = mux( (i & 1.to[I32]) == 1.to[I32], 0.to[I32], 1.to[I32] )
			
			val oe_par_factor = (num_elems - start_i) >> 1
			Foreach(start_i until num_elems - 1 by 2 par oe_par_factor) { k =>
				val value_1 = knn_set(k)
				val value_2 = knn_set(k + 1)
				val smaller_value = mux(value_1 < value_2, value_1, value_2)
				val bigger_value = mux(value_1 > value_2, value_1, value_2)
				knn_set(k) = smaller_value 
				knn_set(k + 1) = bigger_value
			}
		}
	}

	
	def merge_sorted_lists(par_knn_set 		: RegFile1[Int], 
						   par_label_set	: RegFile1[LabelType],
						   sorted_label_set	: RegFile1[LabelType],
						   k_const 			: Int,
						   par_factor  		: Int) : Unit = {

		val partition_index_list = RegFile[Int](par_factor)
		Foreach(par_factor by 1 par par_factor) { p => partition_index_list(p) = 0.to[Int] }

		Foreach(k_const by 1) { k =>
			val insert_this_label =	Reduce(Reg[LabelAndIndex])(par_factor by 1 par par_factor) { p =>
										val check_at_index = partition_index_list(p)
										val current_label = par_label_set(p * k_const + check_at_index)
										val current_dist  = par_knn_set(p * k_const + check_at_index)
										LabelAndIndex(current_dist, current_label, p)
									}{ (a, b) => mux(a.dist < b.dist, a, b) }
			sorted_label_set(k) = insert_this_label.label 
			partition_index_list(insert_this_label.inx) = partition_index_list(insert_this_label.inx) + 1
		}

	} 

		/*
	}
	@virtualize 
	def insertion_sort(knn_set 		: RegFile1[Int],
					   sorted_knn	: RegFile1[Int]) : Unit = {
		
		val k_const = sorted_knn.length 
		val k_value_reg = Reg[Int](0.to[Int])		
		Foreach(k_const by 1){ i =>

			Foreach(k_const by 1) { curr =>
				// knn_set(curr)
			}

		}
	

	}*/

	def knn_vote(knn_set  	: RegFile1[Int],
				 label_list : RegFile1[LabelType],
				 vote_list	: RegFile1[Int]) : LabelType = {

		// will need to do some form of sorting on knn_set here

		Foreach(label_list.length by 1){ i =>
			vote_list( label_list(i).to[I32] ) = vote_list( label_list(i).to[I32] ) + 1
		}

		val best_label = Reg[IntAndIndex](IntAndIndex(0.to[Int], 0.to[I32]))
		best_label.reset

		Reduce(best_label)(vote_list.length by 1) { j =>
			IntAndIndex(vote_list(j), j)
		} {(l1,l2) => mux(l1.value > l2.value, l1, l2) }

		best_label.inx.to[LabelType]
	}


	def initialize_train_data(class_size 	: Int, 
							  training_set_dram_1 : DRAM1[DigitType1], 
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


		val num_training = 18000
		val class_size = 1800 /* number of digits in training set that belong to each class */
		val num_test = 2000

		val run_on_board = true //(args(0).to[Int] > 0.to[Int]).as[Boolean]
		val file_string = if (run_on_board) empty_str else simulate_file_string 

		/* input */
		val training_set_dram_1 =  DRAM[DigitType1](num_training)	
		val training_set_dram_2 =  DRAM[DigitType2](num_training)

		val label_set_dram 	  =  DRAM[LabelType](num_training)

		val test_set_dram_1	  =	 DRAM[DigitType1](num_test)
		val test_set_dram_2	  =	 DRAM[DigitType2](num_test)

		/* output */
		val results_dram	  =  DRAM[LabelType](num_test)

		/* Parameters to tune */
		val k_const 				= 4 /* Number of nearest neighbors to handle */
		val num_test_local_len 		= 20 // num_test /* for on chip memory */
		val num_train_local_len 	= num_training 
		val par_factor 				= 1// 10 /* num_training may need to be divisble by it */

		initialize_train_data(class_size, training_set_dram_1, training_set_dram_2, label_set_dram, file_string)
		initialize_test_data(test_set_dram_1, test_set_dram_2, file_string)		

		val expected_results = loadCSV1D[LabelType]("/home/jcamach2/" + file_string + "DigitRecognition/196data/expected.dat", "\n")
 
		val test_dram = DRAM[Int](20)
		Accel {

			val train_set_local1 	= SRAM[DigitType1](num_train_local_len)
			val train_set_local2 	= SRAM[DigitType2](num_train_local_len)

			val label_set_local 	= SRAM[LabelType](num_train_local_len)

			val test_set_local1 	= SRAM[DigitType1](num_test_local_len)
			val test_set_local2 	= SRAM[DigitType2](num_test_local_len)

			val test_sort = RegFile[Int](20)
			val test_sram = SRAM[Int](20)

			Foreach(0 until 20) { i =>
				test_sort(i) = 20.to[Int] - i.to[Int]
			}
			Pipe { network_sort(test_sort) }

			Foreach(0 until 20) { i =>
				test_sram(i) = test_sort(i) 
			}

			test_dram store test_sram
		/*
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
				Foreach(num_test_local_len by 1) { test_inx =>

					val vote_list		= 	RegFile.buffer[Int](10)

					/* initialize KNN */
					val knn_tmp_large_set = RegFile.buffer[Int](k_const) // when parallelizing, size will need to be k_const * par_factor
					val label_list_tmp 	= 	RegFile.buffer[LabelType](k_const)
					Foreach(k_const by 1 par k_const) { k =>
						knn_tmp_large_set(k) = 256.to[Int]
					}

					val knn_actual	= RegFile[Int](k_const)
					Pipe { initialize(knn_actual, label_list_tmp, vote_list) }

					/* Training Loop */
					Foreach(num_train_local_len by 1 par 1) { train_inx =>
						Pipe { update_knn(test_set_local1(test_inx), test_set_local2(test_inx),
										  train_set_local1(train_inx), train_set_local2(train_inx),
										  knn_tmp_large_set, label_list_tmp, label_set_local(train_inx)) }
					}

					/* Do KNN */
					Pipe { results_local(test_inx) = knn_vote(knn_tmp_large_set, label_list_tmp, vote_list) }

				}

				results_dram(test_factor :: test_factor + num_test_local_len) store results_local 
			} */

		}

		val test_results = getMem(test_dram)

		for (i <- 0 until 20) { println( test_results(i)) }		

		val result_digits = getMem(results_dram)
    
		/* Test */
	//	val cksum =	result_digits.zip(expected_results){ (d1,d2) => if (d1 == d2) 1.0 else 0.0 }.reduce{ _ + _ }
	//print(cksum)
	//	println(" out of " + num_test + " correct.")
	}


}