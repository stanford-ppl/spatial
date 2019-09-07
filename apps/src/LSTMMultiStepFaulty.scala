import spatial.dsl._
import java.io._

@spatial
object LSTMMultiStepFaulty extends SpatialApp {
  // This should take 5180 cycles to run...
  def main(args: Array[String]): Unit = {
    // This is describing the LSTM dataflow
    type T = Float
    type Fix_6 = FixPt[FALSE, _8, _6]
    type Fix_7 = FixPt[FALSE, _7, _7]

    val nHiddenUnits = ArgIn[Int]
    val nFeatures = ArgIn[Int]
    val nTimeSteps = ArgIn[Int]

    setArg(nTimeSteps,args(0).to[Int])
    setArg(nHiddenUnits,args(1).to[Int])
    setArg(nFeatures,args(2).to[Int])

    // TODO: how to set these non-linear functions?
    val tanh: T => T = x => {
      val xval = x.to[Fix_7]
      val idx = xval.bits(I32(0) :: I32(10)).as[I32]
      val lut_k = LUT.fromFile[Float](1024)("./apps/src/tanh_k_float.txt")
      val lut_h = LUT.fromFile[Float](1024)("./apps/src/tanh_h_float.txt")

      val k: Float = lut_k(idx)
      val h: Float = lut_h(idx)
      val y = x * k + h
      y
    }


    val sigmoid: T => T = x => {
      val xval = x.to[Fix_6]
      val idx = xval.bits(I32(0) :: I32(10)).as[I32]
      val lut_k = LUT.fromFile[Float](1024)("./apps/src/sigmoid_k_float.txt")
      val lut_h = LUT.fromFile[Float](1024)("./apps/src/sigmoid_h_float.txt")

      val k: Float = lut_k(idx)
      val h: Float = lut_h(idx)
      val y = x * k + h
      y

    }

    val activationI: T => T = sigmoid
    val activationJ: T => T = tanh
    val activationF: T => T = sigmoid
    val activationO: T => T = sigmoid

    val activations = List(activationI, activationJ, activationF, activationO)

    val hu = 1
    val ru = 4
    val rv = 4

    val loadPar = 1
    val storePar = 1

    // problem-specific params

    //val nHiddenUnits = 128
    //val nFeatures = 128
    //val nTimeSteps = 100
    val nGates = 4

    val cHiddenUnits = 128
    val cFeatures = 128
    val cTimeSteps = 5

    val ijfoDRAMs: scala.List[DRAM2[T]] = scala.List.tabulate(nGates) {
      _ =>
        val re: DRAM2[T] = DRAM[T](cHiddenUnits, cFeatures + cHiddenUnits)
        re
    }

    val ijfoData: scala.List[Matrix[T]] = List.tabulate(nGates) {
      num =>
        //val fileName = "weight_"+num.toString()
        //val writer = new PrintWriter(new File(fileName))

        val re: Matrix[T] = Matrix.tabulate[T](
          cHiddenUnits,
          cFeatures + cHiddenUnits
        ) { (r , c) =>
          var ele = 0.to[T];
          if( r < nHiddenUnits && c < (nFeatures + nHiddenUnits) ) {
            ele = (r*64 + c +num *10000).to[T]
          }
          else {
            ele = 0.to[T]
          }
          //val ele = ((r + c + num).to[T]*0.25).to[T]
          ele
        }
        printMatrix(re, "weight_"+num.toString )
        re
    }
    ijfoDRAMs zip ijfoData foreach {
      case (mem, data) =>
        setMem(mem, data)
        val weight = getMatrix(mem)
        printMatrix(weight,"weight_dram")
    }



    def getVecDRAM(size: I32): DRAM1[T] = {
      val re = DRAM[T](size)
      val reData = Array.tabulate[T](size)(_ => 0.5.to[T])
      setMem(re, reData)
      re
    }

    def getVecDRAMZero(size: I32): DRAM1[T] = {
      val re = DRAM[T](size)
      val reData = Array.tabulate[T](size)(_ => 0.to[T])
      setMem(re, reData)
      re
    }

    def getVecDRAMNum(num: I32, size: I32): DRAM1[T] = {
      val re = DRAM[T](size)
      val reData = Array.tabulate[T](size)(i => ((i + num).to[T]*0.25).to[T])
      //printArray(reData, "bias_"+num.toString)
      setMem(re, reData)
      re
    }

    val cInitDRAM = getVecDRAMZero(cHiddenUnits)
    //val xhDRAM = getVecDRAM((nFeatures + nHiddenUnits).to[I32])
    val xhDRAM = DRAM[T]( cTimeSteps + 1, cFeatures + cHiddenUnits)
    val xhData = (0::cTimeSteps+1, 0::cFeatures+cHiddenUnits){(r,c) =>
      if (r < nTimeSteps && c < nFeatures)
        ((r + c).to[T]*0.25).to[T]
      else 0.to[T]
    }
    //printMatrix(xhData,"xhData")
    setMem(xhDRAM,xhData)


    val biasesDRAM: List[DRAM1[T]] = List.tabulate(nGates) {
      n =>
        val re: DRAM1[T] = getVecDRAMNum(n, cHiddenUnits.to[I32])
        re
    }

    val hResultDRAM: DRAM2[T] = DRAM[T](cTimeSteps,cHiddenUnits)
    val cResultDRAM: DRAM1[T] = DRAM[T](cHiddenUnits)

    Accel {
      // Load all gates weights and x, c, h data in one shot

      //load weight
      val ijfoMems: List[SRAM2[T]] = List.tabulate(nGates) {
        _ =>
          val re: SRAM2[T] =
            SRAM[T](cHiddenUnits, cFeatures + cHiddenUnits)
          re
      }
      ijfoMems zip ijfoDRAMs foreach {
        case (sram, dram) =>
          sram load dram(
            0.to[I32] :: nHiddenUnits,
            0.to[I32] :: (nFeatures + nHiddenUnits).to[I32] par loadPar.to[I32]
          )
      }
      //load bias
      val biasesMems: List[SRAM1[T]] = List.tabulate(nGates) {
        _ =>
          val re: SRAM1[T] = SRAM[T](cHiddenUnits)
          re
      }
      biasesMems zip biasesDRAM foreach {
        case (sram, dram) =>
          sram load dram(0.to[I32] :: nHiddenUnits par loadPar.to[I32])
      }

      //load c
      val c: SRAM1[T] = SRAM[T](cHiddenUnits)
      c load cInitDRAM(0.to[I32] :: nHiddenUnits par loadPar.to[I32])

      //load xh
      val xh: SRAM2[T] = SRAM[T]( (cTimeSteps+1),(cFeatures + cHiddenUnits))
      xh load xhDRAM(0.to[I32] :: (nTimeSteps + 1) , 0.to[I32] :: (nFeatures + nHiddenUnits).to[I32] par loadPar.to[I32])

      //val hNew: SRAM1[T] = SRAM[T](nHiddenUnits)
      //val cNew: SRAM1[T] = SRAM[T](nHiddenUnits)

      Sequential.Foreach(nTimeSteps by 1.to[I32]) { nStep =>
        val lastIterUV: I32 = (((nHiddenUnits + nFeatures) / (ru * rv) - 1) * (ru * rv)).to[I32]
        val accumRegs = List.tabulate(nGates)(_ => Reg[T])
        Foreach(
          nHiddenUnits by 1.to[I32] par hu.to[I32],
          (nHiddenUnits + nFeatures) by (ru * rv).to[I32]
        ) { (ih, iuvTile) =>
          accumRegs.zip(ijfoMems).foreach {
            case (acc, w) =>
              val t = List.tabulate(ru * rv) { ii =>
                val iuv = iuvTile + ii.to[I32]
                //val re: T = (w(ih, iuv) * xh(nStep,iuv)).to[T]
                val re = w(ih, iuv) * xh(nStep,iuv)
                re
              }.sumTree
              acc := mux(iuvTile == 0.to[I32], t, t + acc.value)
          }

        if (iuvTile == lastIterUV) {
          val i :: j :: f :: o :: v =
            accumRegs.zip(biasesMems).zip(activations).map {
              case ((a, b), ac) => ac(a.value + b(ih).to[T])
            }

          val cPrime = i * j + c(ih) * f
          c(ih) = cPrime
          //hNew(ih) = (tanh(cPrime) * o).to[T]
          val xhRow = nStep + 1
          val xhCol = nFeatures + ih.to[I32]
          xh(xhRow,xhCol) = tanh(cPrime) * o
        }
      } //for ih, iuvTile
    } //for nStep

    cResultDRAM(0.to[I32] :: nHiddenUnits par storePar) store c(0.to[I32] :: nHiddenUnits)
    hResultDRAM(0.to[I32] :: nTimeSteps, 0.to[I32] :: nHiddenUnits par storePar) store xh(1.to[I32] :: nTimeSteps+1, nFeatures :: (nFeatures + nHiddenUnits) )
  }

  val cResult = getMem(cResultDRAM)
  val hResult = getMatrix(hResultDRAM)
  printArray(cResult, "c = ")
  printMatrix(hResult, "h = ")


   val tanhHost: Array[T] => Array[T] = x => {
      Array.tabulate[T](x.length)(i => tanh(x(i).to[T]))
      //x
    }
    def sigHost: Array[T] => Array[T] = x => {
      Array.tabulate[T](x.length)(i => sigmoid(x(i).to[T]))
      //x
    }
    def tanhEle(x: T): T = tanh(x)

    val biasesData: List[Tensor1[T]] = biasesDRAM.map(f => getMem(f))
    val xh: Tensor1[T] = getMem(xhDRAM(0::1,0::256))
    val c: Tensor1[T] = getMem(cInitDRAM)
    val gates: List[Array[T]] = ijfoData.zip(biasesData).map {
      case (m: Matrix[_], b: Tensor1[_]) =>
        Array.tabulate[T](nHiddenUnits) { i =>
          Array
            .tabulate[T](nHiddenUnits + nFeatures) { j =>
            m(i, j).to[T] * xh(j)
          }
            .reduce(_ + _) + b(i).to[T]
        }
    }

    val i :: j :: f :: o :: v = List(sigHost, tanhHost, sigHost, sigHost).zip(gates).map{ case (fn, g) => fn(g) }
    val cPrimeGold: Tensor1[T] =
      Array.tabulate[T](nHiddenUnits)(ih => i(ih) * j(ih) + c(ih) * f(ih))

    val hPrimeGold: Tensor1[T] =
      Array.tabulate[T](nHiddenUnits)(ih => tanhEle(cPrimeGold(ih)) * o(ih))

    def variance(a: Tensor1[T], b: Tensor1[T]): T = Array.tabulate[T](nHiddenUnits){ ih =>
      pow(abs(a(ih) - b(ih)), 2)
    }.reduce(_ + _) / 128

    printArray(cPrimeGold, "cGold = ")
    printArray(hPrimeGold, "hGold = ")
    println("var c = " + variance(cResult, cPrimeGold))
    //println("var h = " + variance(hResult(0::1, 0::128), hPrimeGold))
    

  }
}
