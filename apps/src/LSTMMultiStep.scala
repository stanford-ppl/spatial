import spatial.dsl._

@spatial
object LSTMMultiStep extends SpatialApp {
  // This should take 5180 cycles to run...
  def main(args: Array[String]): Unit = {
    // This is describing the LSTM dataflow
    type lowT = FixPt[TRUE, _2, _6]
    type highT = FixPt[TRUE, _10, _22]
    type F = FltPt[_24, _8]

    val controlRegs = List.tabulate(3)(_ => ArgIn[Int])
    controlRegs.zipWithIndex.foreach {
      case (reg, idx) => setArg(reg, args(idx).to[Int])
    }

    val nTimeStepsConfig :: nHiddenUnitsConfig :: nFeaturesConfig :: v =
      controlRegs

    val ru = 2
    val rv = 2
    val nHiddenUnitsDefault = 64
    val nFeaturesDefault = 64
    val nGates = 4

    val ijfoDRAMs: scala.List[DRAM2[lowT]] = scala.List.tabulate(nGates) { _ =>
      val re: DRAM2[lowT] =
        DRAM[lowT](nHiddenUnitsDefault, nFeaturesDefault + nHiddenUnitsDefault)
      re
    }

    val ijfoData: scala.List[Matrix[lowT]] = List.tabulate(nGates) { _ =>
      val re: Matrix[lowT] = Matrix.tabulate[lowT](
        nHiddenUnitsDefault.to[I32],
        (nFeaturesDefault + nHiddenUnitsDefault).to[I32]
      ) { (_, _) =>
        1.to[lowT]
      }
      re
    }

    ijfoDRAMs zip ijfoData foreach {
      case (mem, data) =>
        setMem(mem, data)
    }

    def getVecDRAM[T: Num](size: I32): DRAM1[T] = {
      val re = DRAM[T](size)
      val reData =
        Array.tabulate[T](size)(_ => 1.to[T])
      setMem(re, reData)
      re
    }

    val cInitDRAM = getVecDRAM[highT](nHiddenUnitsDefault.to[I32])
    val xhDRAM =
      getVecDRAM[highT]((nFeaturesDefault + nHiddenUnitsDefault).to[I32])

    val biasesDRAM: List[DRAM1[lowT]] = List.tabulate(nGates) { _ =>
      val re: DRAM1[lowT] = getVecDRAM[lowT](nHiddenUnitsDefault.to[I32])
      re
    }

    val hResultDRAM: DRAM1[highT] = DRAM[highT](nHiddenUnitsDefault)
    val cResultDRAM: DRAM1[highT] = DRAM[highT](nHiddenUnitsDefault)

    // TODO: Input is Float, and reschedule the input based on range?
    Accel {
      // Load all gates weights and x, c, h data in one shot
      val tanh: highT => F = x => {
        val y = x
        y.to[F]
      }

      val activation: highT => F = x => {
        val y = x
        // 0: 0. 249* 0 + 0.5
        // 1024: 0 * 1024 + 1.0
        y.to[F]
      }

      val ijfoActs = List(activation, tanh, activation, activation)
      val ijfoMems: List[SRAM2[lowT]] = List.tabulate(nGates) { _ =>
        val re: SRAM2[lowT] =
          SRAM[lowT](nHiddenUnitsDefault,
                     nFeaturesDefault + nHiddenUnitsDefault)
        re
      }

      ijfoMems zip ijfoDRAMs foreach {
        case (sram, dram) =>
          sram load dram(
            0.to[I32] :: nHiddenUnitsDefault.to[I32],
            0.to[I32] :: (nFeaturesDefault + nHiddenUnitsDefault).to[I32]
          )
      }

      val biasesMems: List[SRAM1[lowT]] = List.tabulate(nGates) { _ =>
        val re: SRAM1[lowT] = SRAM[lowT](nHiddenUnitsDefault)
        re
      }

      biasesMems zip biasesDRAM foreach {
        case (sram, dram) =>
          sram load dram(0.to[I32] :: nHiddenUnitsDefault.to[I32])
      }

      val c: SRAM1[highT] = SRAM[highT](nHiddenUnitsDefault)
      val xh: SRAM1[highT] = SRAM[highT](nFeaturesDefault + nHiddenUnitsDefault)
      c load cInitDRAM(0.to[I32] :: nHiddenUnitsDefault.to[I32])
      xh load xhDRAM(
        0.to[I32] :: (nFeaturesDefault + nHiddenUnitsDefault).to[I32])

      Sequential.Foreach(nTimeStepsConfig.value by 1.to[I32]) { iStep =>
        val lastIterUV: I32 =
          (((nHiddenUnitsConfig + nFeaturesConfig) / (ru * rv) - 1) * (ru * rv))
            .to[I32]
        val accumRegs = List.tabulate(nGates)(_ => Reg[highT])

        Pipe
          .II(ii = 1)
          .Foreach(
            nHiddenUnitsConfig by 1,
            (nHiddenUnitsConfig + nFeaturesConfig) by (ru * rv)
          ) { (ih, iuvTile) =>
            accumRegs.zip(ijfoMems).foreach {
              case (acc, w) =>
                val t =
                  List
                    .tabulate(ru * rv) { ii =>
                      val iuv = iuvTile + ii.to[I32]
                      val re: highT = w(ih, iuv).to[highT] * xh(iuv).to[highT]
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
//                    ac(a.value + b(ih).to[highT])
                    a.value + b(ih).to[highT]
                }

              val cPrime = i * j + c(ih).to[highT] * f
              c(ih) = cPrime.to[highT]
              xh(ih + nFeaturesConfig.value) = cPrime * o
            }
          }

        if (iStep == nTimeStepsConfig.value - 1) {
          cResultDRAM store c(0.to[I32] :: nHiddenUnitsConfig.value.to[I32])
          hResultDRAM store xh(
            nFeaturesConfig.value.to[I32] :: nFeaturesConfig.value
              .to[I32] + nHiddenUnitsConfig.value.to[I32])
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
//      Array.tabulate[highT](x.length)(i => tanh[Float](x(i).to[Float]).to[highT])
      x
    }
    def tanhEle(x: highT): highT = x

    val biasesData: List[Tensor1[lowT]] = biasesDRAM.map(f => getMem(f))
    val xhGold: Tensor1[highT] = getMem(xhDRAM)
    val cGold: Tensor1[highT] = getMem(cInitDRAM)

    for (_ <- nTimeStepsConfig.value by 1) {
      // TODO: I'm not sure if this would double-allocate the memory on the host side.
      val gates: List[Array[highT]] = ijfoData.zip(biasesData).map {
        case (m: Matrix[_], b: Tensor1[_]) =>
          Array.tabulate[highT](nHiddenUnitsConfig) { i =>
            Array
              .tabulate[highT](nHiddenUnitsConfig + nFeaturesConfig) { j =>
                m(i, j).to[highT] * xhGold(j)
              }
              .reduce(_ + _) + b(i).to[highT]
          }
      }

      val i :: j :: f :: o :: vv =
        List(sigHost, tanhHost, sigHost, sigHost).zip(gates).map {
          case (fn, g) => fn(g)
        }

      for (ih <- nHiddenUnitsConfig.value by 1) {
//        val cPrimeGold: Tensor1[highT] =
//          Array.tabulate[highT](nHiddenUnitsConfig)(ih =>
//            i(ih) * j(ih) + c(ih) * f(ih))
//
//        val hPrimeGold: Tensor1[highT] =
//          Array.tabulate[highT](nHiddenUnitsConfig)(ih =>
//            tanhEle(cPrimeGold(ih)) * o(ih))
        cGold(ih) = i(ih) * j(ih) + cGold(ih) * f(ih)
        xhGold(ih + nHiddenUnitsConfig.value) = tanhEle(cGold(ih) * o(ih))
      }
    }

    def variance(a: Tensor1[highT], b: Tensor1[highT]): F =
      Array
        .tabulate[highT](nHiddenUnitsConfig) { ih =>
          pow(abs(cResult(ih) - cGold(ih)), 2)
        }
        .reduce((a, b) => a + b)
        .to[F] / nHiddenUnitsConfig.value.to[F]

    println("var c = " + variance(cResult, cGold))
    println("var h = " + variance(hResult, xhGold))
    printArray(cResult, "cResult = ")
    printArray(hResult, "hResult = ")
    printArray(cGold, "cGold = ")
    printArray(xhGold, "hGold = ")
  }
}
