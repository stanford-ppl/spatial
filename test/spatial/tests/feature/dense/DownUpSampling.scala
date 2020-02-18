package spatial.tests.feature.dense

// # C implementation
//#include<printf.h>
//
//int main(int argc, char** argv) {
//    int inputs[1000];
//    for (int i = 0; i < 1000; i++){
//        inputs[i] = i;
//    }
//    int downsample_result[200];
//    for (int i = 0; i < 200; i++){
//        downsample_result[i] = inputs[i*5];
//    }
//    int upsample_result[800];
//    for (int i = 0; i < 200; i++){
//        for (int j = 0; j < 4; j++){
//            upsample_result[i*4+j] = downsample_result[i];
//        }
//    }
//    for (int i = 0; i < 800; i++){
//        printf("output %d: %d\n", i, upsample_result[i]);
//    }
//    return 0;
//}

object Helper {
  def ifThenElse[T](c1: scala.Int, c2: scala.Int, v1: T, v2: T): T = {
    if (c1 == c2) v1 else v2
  }
}

import spatial.dsl._
import forge.tags._
import spatial.lang.AxiStream256Bus
object Samplers {
  // Dumb version that is just transliterated C, using 2 controllers, no parallelization
  @virtualize
  @api def TwoBoxes[T:Num](
    in: FIFO[T],
    out: FIFO[T],
    downRate: scala.Int,
    upRate: scala.Int,
    tileSize: scala.Int,
    numel: scala.Int
  ): Unit = {
    val down = FIFO[T](tileSize)
    // Q2: Is it fair to deq {downRate} elements per cycle?
    'DOWNSAMP.Foreach(numel by 1){ i =>
      val data = in.deq()
      if (i % downRate == 0) down.enq(data)
    }
    // Q3: Is it fair to enq a vector instead of using a nested cchain?
    'UPSAMP.Foreach(numel/downRate by 1, upRate by 1){ (i,j) =>
      val latch = Reg[T]
      val data = if (j == 0) {val t = down.deq(); latch := t; t} else {latch.value}
      out.enq(data)
    }
  }

  // Smarter implementation using FIFOs, spamming logic, and a single controller with rate-matching
  @virtualize
  @api def OneBox[T:Num](
    in: FIFO[T],
    out: FIFO[T],
    downRate: scala.Int,
    upRate: scala.Int,
    tileSize: scala.Int,
    numel: scala.Int,
    inPar: scala.Int
  ): Unit = {
    assert(upRate <= downRate, "OneBox needs to be rewritten to support upRate > downRate")
    val minInView = scala.math.ceil(({inPar-downRate+1} max 1).toDouble / downRate.toDouble).toInt
    val maxInView = scala.math.ceil(inPar.toDouble / downRate.toDouble).toInt
//    assert(inPar <= downRate, "OneBox needs to be rewritten to support multiple captures per vec during downsampling")
    'SAMPLER.Foreach(numel by inPar){ i =>
      val raw = in.deqVec(inPar)
      val isEn = List.tabulate(inPar){n => if (((i + n) % downRate) == 0.to[I32]) 1.to[I4] else 0.to[I4]}
      val inView = isEn.sumTree
      List.tabulate(maxInView-minInView+1){kk =>
        val k = kk + minInView - 1
        if (inView == (k + 1).to[I4]) {
          val seqData = List.tabulate(k + 1){m =>val takeMask: List[Bit] = List.tabulate(inPar){n => (isEn(n) == 1.to[I4]) && isEn.take(n).sumTree == m.to[I4]}
            List.tabulate(upRate){p => oneHotMux(takeMask, List.tabulate(inPar){i => raw(i)})}
          }.reduce{_++_}
          out.enqVec(Vec.ZeroFirst(seqData:_*))
        }
      }
    }
  }
}

import spatial.dsl._

class DownUpSampleDRAM1 extends DownUpSampleDRAM(1,128)
class DownUpSampleDRAM2 extends DownUpSampleDRAM(2,256)
class DownUpSampleDRAM4 extends DownUpSampleDRAM(4,512)
class DownUpSampleDRAM8 extends DownUpSampleDRAM(8,512)
class DownUpSampleDRAM8OneBox extends DownUpSampleDRAM(8,512) {
  override def backends = Seq(VCS, Zynq, ZCU)
}
class DownUpSampleDRAM8TwoBox extends DownUpSampleDRAM(8,512, true) {
  override def backends = Seq(VCS, Zynq, ZCU)
}
class DownUpSampleDRAM16 extends DownUpSampleDRAM(16,512)
class DownUpSampleDRAM32 extends DownUpSampleDRAM(32,512)

// Test with data coming from DRAM
@spatial abstract class DownUpSampleDRAM(inWidth: scala.Int, tileSize: scala.Int, useTwoBox: scala.Boolean = false) extends SpatialTest {
  override def backends = DISABLED

  def main(args: Array[String]): Unit = {
    val numel = 200
    val downRate = 5
    val upRate = 4
    val signalData = Array.tabulate[I8](numel){i => i.to[I8]}
    val inDRAM = DRAM[I8](numel)
    val outDRAM = DRAM[I8](numel/downRate*upRate)
    setMem(inDRAM, signalData)

    Accel {
      val in = FIFO[I8](tileSize)
      val out = FIFO[I8](tileSize)

      // Q1: Is it fair to just drop {downRate-1} of every {downRate} elements upon loading?
      Stream {
        in load inDRAM(0 :: numel par inWidth)

        if (useTwoBox) Samplers.TwoBoxes[I8](in, out, downRate, upRate, tileSize, numel)
        else Samplers.OneBox[I8](in, out, downRate, upRate, tileSize, numel, inWidth)

        // Q4: Is it fair to do the down and the up sampling in one loop?
        Pipe {
          outDRAM(0 :: numel / 5 * 4) store out
        }
      }
    }

    val gold = Array(List.tabulate(numel/downRate, upRate){(i,j) => signalData(i*downRate)}.flatten:_*)
    val got = getMem(outDRAM)
    printArray(gold, "Wanted:")
    printArray(got, "Got:")
    println(r"PASS: ${gold === got}")
    assert(got === gold)
  }
}