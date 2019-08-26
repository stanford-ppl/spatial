package spatial.tests.Rosetta

import spatial.dsl._
import spatial.targets._

@spatial class SpamFiltering extends SpatialTest {
	//override def runtimeArgs = "1 0"

    case class InferenceStats(tp : Int, tn : Int, fp : Int, fn : Int)


	type DataType 			= FixPt[TRUE, _4, _12] 
	type FeatureType 		= FixPt[TRUE, _13, _19] 
	type LabelType 			= FixPt[FALSE, _8, _0]

	type IdxFixed 			= FixPt[FALSE, _12, _0]
	type LutInFixed			= FixPt[TRUE, _4, _8]

	type TmpFixed 			= FixPt[TRUE, _20, _12]

	type BigFeatureType		= FixPt[TRUE, _32, _19]

	val simulate_file_string 	= "spatial-lang/apps/src/Rosetta/"
	val empty_str = ""

	/* Params */
	val parReduce 		= 4
	val transfer_par  	= 8

	def sigmoid(exponent 	: FeatureType,
				sigmoidLUT	: SRAM1[FeatureType],
				lut_size 	: Int) : FeatureType = {

		val lutIn_twidth = 12 
		val lutIn_iwidth = 4
		val lutOut_twidth = 12 
		val lutOut_iwidth = 2

		if (exponent > 4.to[FeatureType]) {
			1.to[FeatureType]
		} else if (exponent < -4.to[FeatureType]) {
		    0.to[FeatureType]
		} else {

			val in = exponent.to[TmpFixed]
			val use_index = mux(in < 0.0.to[TmpFixed], 
								(lut_size.to[IdxFixed] - (-in.to[TmpFixed ] << (lutIn_twidth - lutIn_iwidth)).to[IdxFixed]),
								(in.to[TmpFixed ] << (lutIn_twidth - lutIn_iwidth)).to[IdxFixed])
			
			sigmoidLUT(use_index.to[I32])
		}
	}


	def dotProduct(theta 	    : SRAM1[FeatureType], 
				   train_vec    : SRAM2[DataType], 
				   id 			: I32) : FeatureType = {

		Reduce(Reg[FeatureType](0.0.to[FeatureType]))(theta.length by 1 par parReduce){ inx =>
			theta(inx) * train_vec(id, inx).to[FeatureType]
		}{_ + _}

	}

	def computeTheta(theta 		: SRAM1[FeatureType], 
					 feature  	: SRAM2[DataType],
					 scale	 	: FeatureType, 
					 id 		: I32, 
					 learning_rate    : Int) : Unit = {


		Foreach(theta.length by 1 par parReduce) { inx => /* foreach feature */
			theta(inx) = theta(inx) - learning_rate.to[FeatureType] * scale * feature(id, inx).to[FeatureType] 
		}
	}

	def main(args: Array[String]): Void = {


		val num_training_samples = 4500
		val num_features 		 = 1024
		val num_testing_samples  = 500
		val num_epochs 			 = 5

		val run_on_board = (args(0).to[Int] > 0.to[Int]).as[Boolean]
		val file_string = if (run_on_board) empty_str else simulate_file_string 


		def initialize_training_set_dram(label_dram			: DRAM1[LabelType],
										 training_set_dram 	: DRAM2[DataType], 
										 num_features 	 	: Int, 
										 num_training_samples : Int) : Unit = {

			val training_set_csv 	= loadCSV1D[DataType]("/home/jcamach2/" + file_string + "SpamFiltering/data/shuffledfeats.dat", "\n")
			val label_set_csv 		= loadCSV1D[LabelType]("/home/jcamach2/" + file_string + "SpamFiltering/data/shuffledlabels.dat", "\n")

	 		val training_set_host 	= Matrix.tabulate(num_features, num_training_samples){ (f,t) =>
											val sample_inx = t * num_features + f 
											training_set_csv.apply(sample_inx).to[DataType] 
										}
			val label_set_host 		= Array.tabulate(num_training_samples) { l =>		
											label_set_csv.apply(l).to[LabelType]
										}		

			val print_train = false 
			if (print_train) {
				printMatrix(training_set_host)
				printArray(label_set_host)
			}

			/* sample id x feature */
			val dram_training_set_data = Matrix.tabulate(num_training_samples, num_features){ (t, f) =>
											training_set_host(f, t).to[DataType] 
										}

			setMem(training_set_dram, dram_training_set_data)
			setMem(label_dram, label_set_host) 
		}

		/* Test helper methods */
		def classify_sample(theta 	: 	Array[FeatureType], 
							sample  : 	Array[DataType]) : Boolean = {
			val dp_result = sample.zip(theta){ (a,b) => a.to[FeatureType] * b}.reduce{_ + _}
			dp_result > 0.0.to[FeatureType]
		}

		def ComputeError(data_set 		: Matrix[DataType], 
						 label_set 		: Array[LabelType], 
						 theta 			: Array[FeatureType],
						 num_features 	: Int, 
						 num_training_samples : Int) : Unit = {

			val indexArr = Array.tabulate(num_training_samples){ i => i }
			val current_inference_stats = indexArr.map{ i => {
															val data_array = Array.tabulate(num_features){ j => data_set(i, j) }
															val label_of_data_array = classify_sample(theta, data_array)
															val correct_label = label_set(i) == 1.0.to[LabelType]
															val tp = if (label_of_data_array  && correct_label)  1.to[Int]  else 0.to[Int]  
															val tn = if (!label_of_data_array && !correct_label) 1.to[Int]  else 0.to[Int]  
															//val fp = if (label_of_data_array  && !correct_label) 1.to[Int]  else 0.to[Int]  
															//val fn = if (!label_of_data_array && correct_label)  1.to[Int]  else 0.to[Int]  
															///val inf = InferenceStats(tp, tn, fp, fn)
															tp + tn }
											  }.reduce{ (a, b) => a + b }//InferenceStats(a.tp + b.tp, a.tn + b.tn, a.fp + b.fp, a.fn + b.fn) }

	     //   val tp = current_inference_stats.tp
	     //   val tn = current_inference_stats.tn	
	       	val correct_samples_classified = current_inference_stats //tp + tn 									  
			println("Finished:")										  
			println("# correct classifications: " + correct_samples_classified)
			println("# total samples: " + num_training_samples)
			//println("Accuracy Percentage: " + (correct_samples_classified.as[Float]/num_training_samples.as[Float]) * 100.0 )
		}


		/* input */
		val training_set_dram 	= DRAM[DataType](num_training_samples, num_features)
		val label_dram		  	= DRAM[LabelType](num_training_samples)


		/* output */
		val theta_dram		  	= DRAM[FeatureType](num_features)

		initialize_training_set_dram(label_dram, training_set_dram, num_features, num_training_samples)

		
		val sigmoid_lut_size 	  =  2048
		val sigmoid_lut_dram 	  =	 DRAM[FeatureType](sigmoid_lut_size)		
		val host_sigmoid_lut_csv  =  loadCSV1D[FeatureType]("/home/jcamach2/" + file_string + "SpamFiltering/data/sigmoid_lut.csv", "\n")
		val host_sigmoid_lut      =	 Array.tabulate(sigmoid_lut_size) { s =>		
											host_sigmoid_lut_csv.apply(s).to[FeatureType]
										}	
		setMem(sigmoid_lut_dram, host_sigmoid_lut)

		val num_training_local  = 20 // num_training_samples
		val step_size 			= 60000 /* same as learning rate */


		Accel {

			val training_sample = SRAM[DataType](num_training_local, num_features)
			val theta_sram	 	= SRAM[FeatureType](num_features).buffer 
			val label_sram 		= SRAM[LabelType](num_training_samples)

			val sigmoidLUT 		= SRAM[FeatureType](sigmoid_lut_size)


			/* Initialize sigmoid LUT */
			sigmoidLUT load sigmoid_lut_dram 
			Foreach(0 until num_features par 1) { t =>
				theta_sram(t) = 0.to[FeatureType]
			}

			Foreach(num_epochs by 1) { _ =>

				Foreach(num_training_samples by num_training_local ) { training_start => 

					Parallel {
						training_sample load  training_set_dram(training_start :: training_start + num_training_local, 0 :: num_features par transfer_par) 
						label_sram 		load  label_dram(training_start :: training_start + num_training_local par transfer_par)
					}

					Foreach(num_training_local by 1) { training_id =>	

						//val dp_result = Reg[FeatureType](0.to[FeatureType])
						val prob_result = Reg[FeatureType](0.to[FeatureType])

						//val gradient_tmp = SRAM[FeatureType](num_features)

						val dp_result   = dotProduct(theta_sram, training_sample, training_id) //}
						Pipe { prob_result := sigmoid(dp_result, sigmoidLUT, sigmoid_lut_size) }
						
						
						val training_label = label_sram(training_id)
						computeTheta(theta_sram, training_sample, (prob_result.value - training_label.to[FeatureType]), training_id, step_size)					
						

					}
				}
			}

			theta_dram(0 :: num_features par transfer_par) store theta_sram
		}

		/* test code*/
		val final_theta = getMem(theta_dram)
		val print_theta = false  
		if (print_theta) {
			printArray(final_theta)
		}

		println("Stats for Training Set: ")
		ComputeError(getMatrix(training_set_dram), getMem(label_dram), final_theta, num_features, num_training_samples)

		/* Test Set */
		val test_set_csv 			= loadCSV1D[DataType]("/home/jcamach2/" + file_string + "SpamFiltering/data/newfeats.dat", "\n")
		val test_label_set_csv 		= loadCSV1D[LabelType]("/home/jcamach2/" + file_string + "SpamFiltering/data/newlabels.dat", "\n")
		val test_set_host 			= Matrix.tabulate(num_testing_samples, num_features){ (t,f) =>
											val sample_inx = t * num_features + f 
											test_set_csv.apply(sample_inx).to[DataType] 
										}
		val test_label_set_host 	= Array.tabulate(num_testing_samples) { l =>		
											test_label_set_csv.apply(l).to[LabelType]
										}

		println()
		println("Stats for Test Set: ")										
		ComputeError(test_set_host, test_label_set_csv, final_theta, num_features, num_testing_samples)			
	}
}