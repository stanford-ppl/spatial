import spatial.dsl._
import scala.io.Source


@spatial
object LSTM extends SpatialApp {

	type dtype = FixPt[TRUE, _24, _8]

	def main(args: Array[String]): Unit = {
		// define all dimensions
		// dimension of input matrix x_t (DxnSteps)
                
		val D = 159
		val Ddiff = 1
		// dimension of hidden state h_t (Hx1)
		val H1 = 32
		val H2 = 64
		val H3 = 96
		val H4 = 128
		val BlockSize = 5
		val nSteps = 2
		val Blminus = BlockSize-nSteps
		val nLayers = 2

		val X_file = loadCSV2D[dtype]	("Data15.1m.comp.train.s.csv",",","\n")// load test sequence from file.
		val Wx_file10 = loadCSV2D[dtype]("Data15.1m.comp.ih10.csv",",","\n")// load Wx weights from file.
		val Wh_file10 = loadCSV2D[dtype]("Data15.1m.comp.hh10.csv",",","\n")// load Wh weights from file.
		val Wx_file11 = loadCSV2D[dtype]("Data15.1m.comp.ih11.csv",",","\n")// load Wx weights from file.
		val Wh_file11 = loadCSV2D[dtype]("Data15.1m.comp.hh11.csv",",","\n")// load Wh weights from file.
		val b_file = loadCSV2D[dtype]	("Data15.1m.comp.b.csv",",","\n")
		val mean = loadCSV2D[dtype]		("Data15.1m.comp.mean.csv",",","\n")
		val covar = loadCSV2D[dtype]	("Data15.1m.comp.covar.csv",",","\n")
		val sig_values = loadCSV1D[dtype]("sigmoid_lut.csv", ",")
		val tanh_values = loadCSV1D[dtype]("tanh_lut.csv", ",")
		val sig_DRAM = DRAM[dtype](128)
		val tanh_DRAM = DRAM[dtype](128)
	//	setMem(sig_DRAM, sig_values)
	//	setMem(tanh_DRAM, tanh_values)

		//val zeros = loadCSV2D[dtype]("Zeros.csv",",","\n")
		//val zeros2 = loadCSV2D[dtype]("Zeros2.csv",",","\n")
		val X_t = DRAM[dtype](11,D)
		//val zeros_d = DRAM[dtype](nSteps,nSteps,D)
		//val zeros2_d = DRAM[dtype](D,H1)

		val Wx_10_d = DRAM[dtype](H4, H1)
		val Wx_11_d = DRAM[dtype](H4, H1)
		val Wh_10_d = DRAM[dtype](H4, H1)
		val Wh_11_d = DRAM[dtype](H4, H1)
		val b_f_d = DRAM[dtype](4,H4)
	//setMem(X_t, X_file)
	//setMem(Wx_10_d, Wx_file10)
	//setMem(Wx_11_d, Wx_file11)
	//setMem(Wh_10_d, Wh_file10)
	//setMem(Wh_11_d, Wh_file11)
		val XFlength = 10//X_file.length
	//	setMem(b_f_d, b_file)
		//setMem(zeros_d, zeros)
		//setMem(zeros2_d, zeros2)

		val pr_d = DRAM[dtype](X_file.length,nSteps,Ddiff)
		val re_d = DRAM[dtype](X_file.length,nSteps,Ddiff)
		val er_d = DRAM[dtype](X_file.length,nSteps,Ddiff)

		Accel {
			// sigmoid and tanh LUTs. juan said we can initialize LUTs from DRAM, but unable at the moment. SRAM is quick fix.
			val sigmoid_SRAM = SRAM[dtype](128)
			val tanh_SRAM = SRAM[dtype](128)
			sigmoid_SRAM load sig_DRAM
			tanh_SRAM load tanh_DRAM


			def sigmoid(x:SRAM1[dtype],start:I32, y:SRAM1[dtype]) = {
				Foreach(0 until start) { d0 =>
					val raw_value = x(d0)
					if(raw_value < -5.0){
						y(d0) = sigmoid_SRAM(0)
					} else if(raw_value > 5.0) {
						y(d0) = sigmoid_SRAM(127)
					} else {
						val index = 12.8*raw_value + 64 // address-translation mapping range [-5,5] to [0,128)
						y(d0) = sigmoid_SRAM(index.to[I32]) // need a flooring technique to ensure that indices are integers.
					}
				}
			}
			def tanh(x:SRAM1[dtype],start:I32, y:SRAM1[dtype]) = {
				Foreach(0 until start) { d0 =>
					val raw_value = x(d0)
					if(raw_value < -5.0){
						y(d0) = tanh_SRAM(0)
					} else if(raw_value > 5.0) {
						y(d0) = tanh_SRAM(127)
					} else {
						val index = 32*raw_value + 64 // address-translation mapping range [-2,2] to [0,128)
						y(d0) = tanh_SRAM(index.to[I32]) // need a flooring technique to ensure that indices are integers.
					}
				}
			}
			def activation(Wx:SRAM2[dtype], Wh: SRAM2[dtype], bx:SRAM1[dtype], bh:SRAM1[dtype], X:SRAM2[dtype], H:SRAM2[dtype], C :SRAM2[dtype], fringe :Int , t:Int,i:SRAM1[dtype], f:SRAM1[dtype], o:SRAM1[dtype], g:SRAM1[dtype], l:Int,C_empty:Boolean) = {
				val a_x = SRAM[dtype](H4)
				val a_h = SRAM[dtype](H4)
				val a_t = SRAM[dtype](H4)
				
				if(C_empty == 0.to[Boolean]){
					Foreach(0 until H4) { row => 
						a_x(row) = Reduce(Reg[dtype])(0 until H1) { col =>
							Wx(row, col)*X(t,fringe)+bx(row)
						} {(a,b) => a+b}
						a_h(row) = Reduce(Reg[dtype])(0 until H1) { col =>
							Wh(row, col)*H(fringe,col)+bh(row)
						} {(a,b) => a+b}
						a_t(row) = a_x(row) + a_h(row)
					}
				}else{
					Foreach(0 until H4) { row => 
						a_x(row) = Reduce(Reg[dtype])(0 until H1) { col =>
							Wx(row, col)*X(t,fringe)+bx(row)
						} {(a,b) => a+b}
						a_h(row) = Reduce(Reg[dtype])(0 until H1) { col =>
							Wh(row, col)*X(t,fringe)+bh(row)
						} {(a,b) => a+b}
						a_t(row) = a_x(row) + a_h(row)
					}
				}		

				sigmoid(a_t,0, i)
				sigmoid(a_t,H1, f)
				sigmoid(a_t,H2, g)
				tanh(a_t,H3, o)
				if(C_empty == 0.to[Boolean]){
					val temp = SRAM[dtype](H1)
					Foreach(0 until H1){x =>
						C(fringe.to[I32],x) = f(x)*C(fringe.to[I32],x) + i(x)*g(x)
						temp(x) = C(fringe.to[I32],x)
					}

					tanh(temp,H1,f)

					Foreach(0 until H1){x =>
						H(fringe,x) = o(x)*f(x)
					}
				}else{
					Foreach(0 until H1){x =>
						C(fringe.to[I32],x) = i(x)*g(x)
						H(fringe,x) = 0.to[dtype]
					}
				}
							
			}

			// activation functions
			//val score = SRAM[dtype](D,X_file.length)
			//Sequential.Foreach(0 until D) {r =>
				//Sequential.Foreach(0 until X_file.length) {c =>
					//val sigmoid = LUT[dtype]() // dimensions?
					//val tanh = LUT[dtype]()
			val Wx_10 = SRAM[dtype](H4, H1)
			val Wx_11 = SRAM[dtype](H4, H1)
			val Wh_10 = SRAM[dtype](H4, H1)
			val Wh_11 = SRAM[dtype](H4, H1)
			//val b_f = SRAM[dtype](4,H4)
			val b_f0 = SRAM[dtype](H4)
			val b_f1 = SRAM[dtype](H4)
			val b_f2 = SRAM[dtype](H4)
			val b_f3 = SRAM[dtype](H4)


			//b_f load b_f_d
			b_f0 load b_f_d(0.to[I32],0.to[I32]::H4)
			b_f1 load b_f_d(1.to[I32],0.to[I32]::H4)
			b_f2 load b_f_d(2.to[I32],0.to[I32]::H4)
			b_f3 load b_f_d(3.to[I32],0.to[I32]::H4)

			Wx_10 load Wx_10_d
			Wx_11 load Wx_11_d
			Wh_10 load Wh_10_d
			Wh_11 load Wh_11_d

			

			val h_t_r = SRAM[dtype](D,H1)
			val c_r = SRAM[dtype](D,H1)


			val pr_s = SRAM[dtype](Blminus,nSteps,Ddiff).buffer
			// Foreach(0 until nSteps) {j =>
			// 	Foreach(0 until nSteps) {k =>
			// 		Foreach(0 until D) {l =>
			// 			pr_s(j,k,l) = 0
			// 		}
			// 	}
			// }
			//pr_s(0::nSteps,0::nSteps,0::D) load zeros_d

			Sequential.Foreach(0 until XFlength by Blminus){ b =>
				val X_s = SRAM[dtype](BlockSize,Ddiff)

				val Btrue = mux(b+BlockSize > XFlength, XFlength, b+BlockSize)

				X_s load X_t(b::Btrue,0::Ddiff)

				
				val re_s = SRAM[dtype](Blminus,nSteps,Ddiff)
				val er_s = SRAM[dtype](Blminus,nSteps,Ddiff)
				Sequential.Foreach(0 until Btrue-b-nSteps) { t =>
					// Foreach(0 until D) {j =>
					// 	Foreach(0 until H1) {i =>
					// 		c_r(j,i) = 0 //can load but might be more expensive
					// 		h_t_r(j,i) = X_s(t,j)
					// 	}
					// }
					Foreach(0 until Ddiff){fringe =>
						pr_s(t,0,fringe) = X_s(t,fringe)
						Sequential.Foreach(1 until nSteps){s =>
							Sequential.Foreach(0 until nLayers) { l =>
								val i = SRAM[dtype](H1)
								val f = SRAM[dtype](H1)
								val o = SRAM[dtype](H1)
								val g = SRAM[dtype](H1)
								if(l == 0){
									if(s == 1){
										activation(Wx_10, Wh_10, b_f0, b_f1,X_s,h_t_r,c_r,fringe,t+s, i, f, o, g, l,1.to[Boolean])
									}else{
										activation(Wx_10, Wh_10, b_f0, b_f1,X_s,h_t_r,c_r,fringe,t+s, i, f, o, g, l,0.to[Boolean])
									}
								}else{
									activation(Wx_11, Wh_11, b_f2, b_f3,X_s,h_t_r,c_r,fringe,t+s, i, f, o, g, l,0.to[Boolean])
								}
								//mux(l == 0, activation(Wx_10, Wh_10, b_f0, b_f1,X_s,h_t_r,c_r,fringe,t+s, i, f, o, g, l), activation(Wx_11, Wh_11, b_f2, b_f3,X_s,h_t_r,c_r,fringe,t+s, i, f, o, g, l))
							}
							val temp2 = Reg[dtype](0)
							Reduce(temp2)(0 until H1) { col =>
									h_t_r(fringe,col)
							} {(a,b) => a+b}

							pr_s(t,s,fringe) = temp2/32
						}
						Sequential.Foreach(0 until nSteps){s =>
							if(t+s< nSteps){
								if(b == 0){
									re_s(t,s,fringe) = 0
								}else{
									re_s(t,s,fringe) = pr_s(t+s+Blminus-nSteps,nSteps-1-s,fringe)
								}
							}else{
								re_s(t,s,fringe) = pr_s(t+s-nSteps,nSteps-1-s,fringe) //some process is done to convert the layers to a number
							}			
							er_s(t,s,fringe) = re_s(t,s,fringe) - X_s(t,fringe)
						}
					}
				}
				val beginminus:I32 = b.to[I32]
				val endminus:I32 = Btrue-nSteps
				pr_d(beginminus::endminus,0::nSteps,0::Ddiff) store pr_s(0::Btrue-b-nSteps,0::nSteps,0::Ddiff)
				re_d(beginminus::endminus,0::nSteps,0::Ddiff) store re_s(0::Btrue-b-nSteps,0::nSteps,0::Ddiff)
				er_d(beginminus::endminus,0::nSteps,0::Ddiff) store er_s(0::Btrue-b-nSteps,0::nSteps,0::Ddiff)
				// if(Btrue != XFlength){
				// 	Foreach(0 until nSteps) {j =>
				// 		Foreach(0 until nSteps) {k =>
				// 			Foreach(0 until D) {l =>
				// 				pr_s(j,k,l) = pr_s(j+Blminus,k,l)
				// 			}
				// 		}
				// 	}
				// }
				//pr_s(0::nSteps,) = pr_s(Blminus::BlockSize) 	

					// val temp1 = er(c)-mean(c)
					// val temp2 = //take inverse of covar
					// Foreach(0 until 10){ i =>
					// 	val out1 = Reg[T](0)						
					// 	Reduce(out1)(0 until 10){j =>
					// 		temp2(j)(i)*temp1(i)
					// 	}{_+_}
					// 	score(i) = out1
					// }
					// val result = Reg[T](0)						
					// Reduce(out)(0 until 10){i =>
					// 	score(i)*temp1(i)
					// }{_+_}
					// score(r,c) = result
			}
		}



		for(a <- 0 to Ddiff){
			val pr_t = getMatrix(pr_d(0.to[I32]::XFlength,0.to[I32]::nSteps.to[I32],a.to[I32]))
			val re_t = getMatrix(re_d(0.to[I32]::XFlength,0.to[I32]::nSteps.to[I32],a.to[I32]))
			//val er_t = getMatrix(er_d(0.to[I32]::XFlength,0.to[I32]::nSteps.to[I32],a.to[I32]))
			val er_t = getMatrix(Wx_11_d)
			writeCSV2D[dtype](pr_t,"Results_PR"+a.toString+".csv")
			writeCSV2D[dtype](re_t,"Results_RE"+a.toString+".csv")
			writeCSV2D[dtype](er_t,"Results_ER"+a.toString+".csv")
		}
		// Prediction analysis goes here. Need to finalize problem state, and what is getting "predicted." Need to look 
		// at software model and the data. Still don't have access to it.
	}
}
