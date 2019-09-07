import spatial.dsl._

@spatial
object LSTM extends SpatialApp {
  // This should take 5180 cycles to run...
  def main(args: Array[String]): Unit = {
    // This is describing the LSTM dataflow
    type lowT = FixPt[TRUE, _2, _6]
    type highT = FixPt[TRUE, _10, _22]
    type F = FltPt[_24, _8]

    val nTimeSteps = ArgIn[I32]
    setArg(nTimeSteps, args(0).to[I32])

    val tanh: highT => F = x => {
      val y = x
      y.to[F]
    }

    val activation: highT => F = x => {
      val y = x
      y.to[F]
    }

    val ijfoActs = List(activation, tanh, activation, activation)

    val hu = 1
    val ru = 4
//    val rv = 16
    val rv = 4

    // problem-specific params
    val nHiddenUnits = 128
    val nFeatures = 128
//    val nTimeSteps = 1
    val nGates = 4

    val ijfoDRAMs: scala.List[DRAM2[lowT]] = scala.List.tabulate(nGates) { _ =>
      val re: DRAM2[lowT] = DRAM[lowT](nHiddenUnits, nFeatures + nHiddenUnits)
      re
    }

    val ijfoData: scala.List[Matrix[lowT]] = List.tabulate(nGates) { _ =>
      val re: Matrix[lowT] = Matrix.tabulate[lowT](
        nHiddenUnits.to[I32],
        (nFeatures + nHiddenUnits).to[I32]
      ) { (_, _) =>
//          random[lowT](1.to[lowT])
        0.03125.to[lowT]
      }
      re
    }

    ijfoDRAMs zip ijfoData foreach {
      case (mem, data) =>
        setMem(mem, data)
    }

    def getVecDRAM[T: Num](size: I32): DRAM1[T] = {
      val re = DRAM[T](size)
      val reData = Array.tabulate[T](size)(_ => 0.03125.to[T])
      setMem(re, reData)
      re
    }

    val cInitDRAM = getVecDRAM[highT](nHiddenUnits.to[I32])
    val xDRAM = getVecDRAM[highT](nFeatures)
    val hDRAM = getVecDRAM[highT](nFeatures)

    val biasesDRAM: List[DRAM1[lowT]] = List.tabulate(nGates) { _ =>
      val re: DRAM1[lowT] = getVecDRAM[lowT](nHiddenUnits.to[I32])
      re
    }

    val hResultDRAM: DRAM1[highT] = DRAM[highT](nHiddenUnits)
    val cResultDRAM: DRAM1[highT] = DRAM[highT](nHiddenUnits)

    Accel {
      val ijfoMems: List[SRAM2[lowT]] = List.tabulate(nGates) { _ =>
        val re: SRAM2[lowT] =
          SRAM[lowT](nHiddenUnits, nFeatures + nHiddenUnits)
        re
      }

      ijfoMems zip ijfoDRAMs foreach {
        case (sram, dram) =>
          sram load dram(
            0.to[I32] :: nHiddenUnits.to[I32],
            0.to[I32] :: (nFeatures + nHiddenUnits).to[I32]
          )
      }

      val biasesMems: List[SRAM1[lowT]] = List.tabulate(nGates) { _ =>
        val re: SRAM1[lowT] = SRAM[lowT](nHiddenUnits)
        re
      }

      biasesMems zip biasesDRAM foreach {
        case (sram, dram) =>
          sram load dram(0.to[I32] :: nHiddenUnits.to[I32])
      }

      val x: SRAM1[highT] = SRAM[highT](nFeatures)
      val c: SRAM1[highT] = SRAM[highT](nHiddenUnits)
      val h: SRAM1[highT] = SRAM[highT](nHiddenUnits)
      val cNew: SRAM1[highT] = SRAM[highT](nHiddenUnits)
      val hNew: SRAM1[highT] = SRAM[highT](nHiddenUnits)

      x load xDRAM(0 :: nHiddenUnits)
      h load hDRAM(0 :: nHiddenUnits)
      c load hDRAM(0 :: nHiddenUnits)

      Sequential.Foreach(nTimeSteps by 1.to[I32]) { iStep =>
        val lastIterUV: I32 =
          (((nHiddenUnits + nFeatures) / (ru * rv) - 1) * (ru * rv)).to[I32]
        val accumRegs = List.tabulate(nGates)(_ => Reg[highT])

        Foreach(
          nHiddenUnits.to[I32] by 1.to[I32],
          (nHiddenUnits + nFeatures).to[I32] by (ru * rv).to[I32]
        ) { (ih, iuvTile) =>
          accumRegs.zip(ijfoMems).foreach {
            case (acc, w) =>
              val t =
                List
                  .tabulate(ru * rv) { ii =>
                    val iuv = iuvTile + ii.to[I32]
                    val iuvOffset = iuv - nFeatures
                    val re: highT = w(ih, iuv).to[highT] * mux(
                      iuv < nFeatures, x(iuv), h(iuvOffset)
                    ).to[highT]
                    re
                  }
                  .sumTree
              acc := mux(
                iuvTile == 0.to[I32],
                t,
                t + acc.value
              )
          }

          if (iuvTile == lastIterUV) {
            val i :: j :: f :: o :: v =
              accumRegs.zip(biasesMems).zip(ijfoActs).map {
                case ((a, b), ac) =>
                // ac(a.value + b(ih).to[highT])
                  a.value + b(ih).to[highT]
              }

            val cPrime = i * j + c(ih).to[highT] * f
            cNew(ih) = cPrime
            hNew(ih) = cPrime * o
          }
        }

        Foreach(nHiddenUnits by ru * rv) { i =>
          List.tabulate(ru * rv) { ii =>
            h(i + ii) = hNew(i + ii)
            c(i + ii) = cNew(i + ii)
          }
        }

        if (iStep == nTimeSteps - 1) {
          cResultDRAM store c(0 :: nHiddenUnits)
          hResultDRAM store h(0 :: nFeatures)
        }
      }
    }

    val cResult = getMem(cResultDRAM)
    val hResult = getMem(hResultDRAM)
    val tanhHost: Array[highT] => Array[highT] = x => {
//      Array.tabulate[highT](x.length)(i => tanh[Float](x(i).to[Float]).to[highT])
      x
    }
    def sigHost: Array[highT] => Array[highT] = x => {
      x
    }
    def tanhEle(x: highT): highT = x

    val biasesData: List[Tensor1[lowT]] = biasesDRAM.map(f => getMem(f))
    val x: Tensor1[highT] = getMem(xDRAM)
    val h: Tensor1[highT] = getMem(hDRAM)
    val c: Tensor1[highT] = getMem(cInitDRAM)
    val gates: List[Array[highT]] = ijfoData.zip(biasesData).map {
      case (m: Matrix[_], b: Tensor1[_]) =>
        Array.tabulate[highT](nHiddenUnits) { i =>
          Array
            .tabulate[highT](nFeatures + nHiddenUnits) { j =>
              val a: highT = if (j < nFeatures.to[I32]) x(j) else h(j - nFeatures)
              m(i, j).to[highT] * a
            }
            .reduce(_ + _) + b(i).to[highT]
        }
    }

    val i :: j :: f :: o :: v = List(sigHost, tanhHost, sigHost, sigHost).zip(gates).map{ case (fn, g) => fn(g) }
    val cPrimeGold: Tensor1[highT] =
      Array.tabulate[highT](nHiddenUnits)(ih => i(ih) * j(ih) + c(ih) * f(ih))

    val hPrimeGold: Tensor1[highT] =
      Array.tabulate[highT](nHiddenUnits)(ih => tanhEle(cPrimeGold(ih)) * o(ih))

    def variance(a: Tensor1[highT], b: Tensor1[highT]): highT = Array.tabulate[highT](nHiddenUnits){ ih =>
      pow(abs(cResult(ih) - cPrimeGold(ih)), 2)
    }.reduce((a, b) => a + b) / nHiddenUnits

    println("var c = " + variance(cResult, cPrimeGold))
    println("var h = " + variance(hResult, hPrimeGold))
    printArray(cResult, "cResult = ")
    printArray(hResult, "hResult = ")
    printArray(cPrimeGold, "cPrimeGold = ")
    printArray(hPrimeGold, "hPrimeGold = ")
  }
}
