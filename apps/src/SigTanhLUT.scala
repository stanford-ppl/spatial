import spatial.dsl._

@spatial
object SigTanhLUT extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _10, _22]
//    type highPrecT = FixPt[TRUE, _5, _27]
    type F = FltPt[_24, _8]
    type highPrecT = F

    val tanhLUTLen: scala.Int = 256
    val sigLUTLen: scala.Int = 256

    val sigHostFn: scala.Double => scala.Double = x => {
      1.toDouble / (1.toDouble + scala.math.exp(-x))
    }

    val tanhRange: scala.Int = 4
    val sigRange: scala.Int = 6
    val testRange: scala.Int = 10
    val nTestPoints: scala.Int = 256

    val sigFn: scala.Double => scala.Double = x => {
      1 / ( 1 + scala.math.exp(-x))
    }

    val testInputRawData: List[scala.Double] = {
      val space: scala.Double = testRange * 2.toDouble / nTestPoints
      List.tabulate[scala.Double](nTestPoints)(i => i.toDouble * space - testRange)
    }

    val testInputData: Array[T] = Array.fromSeq[T](testInputRawData.map(a => a.to[T]))

    val testInputDRAM: DRAM1[T] = {
      val d: DRAM1[T] = DRAM[T](nTestPoints)
      setMem(d, testInputData)
      d
    }

    val testOutputTanhDRAM: DRAM1[F] = DRAM[F](nTestPoints)
    val testOutputSigDRAM: DRAM1[F] = DRAM[F](nTestPoints)

    Accel {
      val lutFn: (
        T, scala.Double => scala.Double, scala.Int, scala.Int, scala.Int, scala.Int, (T, F) => F
        ) => F = (x, f, r, ll, lb, hb, invFn) => {
        // TODO: May want to smooth the curve by lattice regression.
        // TODO: Interpolation result is pretty bad close to zero.
        val lut = LUT[F](ll)(Seq.tabulate[F](ll)(i => f(i)): _*)
        val lutOut: F = lut(
          {
            val space: scala.Double = r.toDouble / ll.toDouble
            val idx = (abs(x) / space.to[T]).to[I32]
            idx
          }
        )
        mux(
          x <= (- r).to[T], lb.to[F], mux(
            x >= r.to[T], hb.to[F], invFn(x, lutOut)
          )
        )
      }

      val testInputMem: SRAM1[T] = SRAM[T](nTestPoints)
      val testOutputTanhMem: SRAM1[F] = SRAM[F](nTestPoints)
      val testOutputSigMem: SRAM1[F] = SRAM[F](nTestPoints)

      testInputMem load testInputDRAM

      Foreach (nTestPoints by 1.to[I32]) { idx =>
        val i = testInputMem(idx)

        val rv = lutFn(
          i, x => sigFn(x / sigLUTLen.toDouble * sigRange.toDouble),
          sigRange, sigLUTLen, 0, 1, (i, j) => mux(i < 0, 1.to[highPrecT] - j, j).to[F]
        )
        testOutputSigMem(idx) = rv

         val tv = lutFn(
          i, x => scala.math.tanh(x / tanhLUTLen.toDouble * tanhRange.toDouble),
          tanhRange, tanhLUTLen, -1, 1, (i, j) => mux(i < 0, -j, j).to[F]
        )

        testOutputTanhMem(idx) = tv
      }

      testOutputSigDRAM store testOutputSigMem
      testOutputTanhDRAM store testOutputTanhMem
    }

    def variance(a: Tensor1[F], b: Tensor1[F]): F = Array.tabulate[F](a.length){ ih =>
      pow(abs(a(ih) - b(ih)), 2)
    }.reduce((i, j) => i + j) / a.length.to[F]

    val sigGold: Array[F] = Array.fromSeq[F](testInputRawData.map(i => sigHostFn(i).to[F]))
    val tanhGold: Array[F] = Array.fromSeq[F](testInputRawData.map(i => scala.math.tanh(i).to[F]))
    println("var sig = " + variance(sigGold, getMem(testOutputSigDRAM)))
    println("var tanh = " + variance(tanhGold, getMem(testOutputTanhDRAM)))
    printArray(sigGold, "sigGold = ")
    printArray(getMem(testOutputSigDRAM), "sig = ")
    println("")
    printArray(tanhGold, "tanhGold = ")
    printArray(getMem(testOutputTanhDRAM), "tanh = ")
  }
}
