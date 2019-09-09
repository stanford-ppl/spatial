import spatial.dsl._

@spatial
object LSTM extends SpatialApp {
  // This should take 5180 cycles to run...
  def main(args: Array[String]): Unit = {
    type lowT = FixPt[TRUE, _2, _6]
    type highT = FixPt[TRUE, _10, _22]
    type F = FltPt[_24, _8]

    val controlRegs = List.tabulate(3)(_ => ArgIn[I32])
    val testArgs = List(2, 64, 64)
//    controlRegs.zipWithIndex.foreach(t => setArg(t._1, args(t._2).to[I32]))
//    controlRegs.zipWithIndex.foreach(t => setArg(t._1, testArgs(t._2).to[I32]))
    setArg(controlRegs.head, 2)
    setArg(controlRegs(1), 64)
    setArg(controlRegs(2), 64)

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

    val nHiddenUnits = 128
    val nFeatures = 128
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

      val nTimeSteps :: nFeaturesConfig :: nHiddenUnitsConfig :: nn = controlRegs.map(r => r.value)
      Sequential.Foreach(nTimeSteps by 1) { iStep =>
        val innerBound: I32 = nHiddenUnitsConfig + nFeaturesConfig
        val lastIterUV: I32 =
          ((innerBound / (ru * rv) - 1) * (ru * rv)).to[I32]
        val accumRegs = List.tabulate(nGates)(_ => Reg[highT])

        Pipe.II(1).Foreach(nHiddenUnitsConfig by 1, innerBound by ru * rv) { (ih, iuvTile) =>
          accumRegs.zip(ijfoMems).foreach {
            case (acc, w) =>
              val t =
                List
                  .tabulate(ru * rv) { ii =>
                    val iuv = iuvTile + ii.to[I32]
                    val iuvOffset = iuv - nFeaturesConfig
                    val re: highT = w(ih, iuv).to[highT] * mux(
                      iuv < nFeaturesConfig, x(iuv), h(iuvOffset)
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
            val i :: j :: f :: o :: vv =
              accumRegs.zip(biasesMems).zip(ijfoActs).map {
                case ((a, b), ac) =>
//                  ac(a.value + b(ih).to[highT])
                  a.value + b(ih).to[highT]
              }

            val cPrime = i * j + c(ih) * f
            cNew(ih) = cPrime
            hNew(ih) = cPrime * o
          }
        }

        Pipe.II(1).Foreach(nHiddenUnitsConfig by ru * rv) { i =>
          List.tabulate(ru * rv) { ii =>
            h(i + ii) = hNew(i + ii)
            c(i + ii) = cNew(i + ii)
          }
        }

        if (iStep == nTimeSteps - 1) {
          cResultDRAM store cNew(0 :: nHiddenUnits)
          hResultDRAM store hNew(0 :: nHiddenUnits)
        }
      }
    }

    def getPartialArrayHead[T:Num](array: Tensor1[T], len: I32): Array[T] = {
      Array.tabulate[T](len)(i => array(i))
    }

    val nt :: nh :: nf :: nn = controlRegs.map(a => a.value)
    val tanhHost: Array[highT] => Array[highT] = x => {
      x
    }
    def sigHost: Array[highT] => Array[highT] = x => {
      x
    }
    def tanhEle(x: highT): highT = x

    val biasesData: List[Tensor1[lowT]] = biasesDRAM.map(f => getMem(f))
    val x: Tensor1[highT] = getMem(xDRAM)
    val hGold: Tensor1[highT] = getMem(hDRAM)
    val cGold: Tensor1[highT] = getMem(cInitDRAM)

    for (_ <- nt by 1) {
      val gates: List[Array[highT]] = ijfoData.zip(biasesData).map {
        case (m: Matrix[_], b: Tensor1[_]) =>
          Array.tabulate[highT](nh) { i =>
            Array
              .tabulate[highT](nh + nf) { j =>
                val a: highT = if (j < nf) x(j) else hGold(j - nf)
                m(i, j).to[highT] * a
              }
              .reduce(_ + _) + b(i).to[highT]
          }
      }

      val i :: j :: f :: o :: v = List(
        sigHost, tanhHost, sigHost, sigHost
      ).zip(gates).map{ case (fn, g) => fn(g) }

      for (ih <- nh by 1) {
        cGold(ih) = i(ih) * j(ih) + cGold(ih) * f(ih)
        hGold(ih) = tanhEle(cGold(ih) * o(ih))
      }
    }

    def variance(a: Tensor1[highT], b: Tensor1[highT]): highT = Array.tabulate[highT](nh){ ih =>
      pow(abs(a(ih) - b(ih)), 2)
    }.reduce((a, b) => a + b) / nHiddenUnits

    printArray(getPartialArrayHead(getMem(cResultDRAM), nh), "cResult = ")
    printArray(getPartialArrayHead(getMem(hResultDRAM), nh), "hResult = ")
    printArray(getPartialArrayHead(cGold, nh), "cResultProbe = ")
    printArray(getPartialArrayHead(hGold, nh), "hResultProbe = ")
  }
}
