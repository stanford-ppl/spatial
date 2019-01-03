package spatial.tests.feature.buffering

import spatial.dsl._

// TODO: Make this actually check a bubbled NBuf (i.e.- s0 = wr, s2 = wr, s4 =rd, s1s2 = n/a)
// because I think this will break the NBuf SM since it won't detect drain completion properly
@spatial class BufferRecalc extends SpatialTest {
  def main(args: Array[String]): Void = {
    val N = 2

    val argout = ArgOut[Int]
    val dram = DRAM[Int](32)
    Accel{
      val mem = SRAM[Int](32).buffer
      Foreach(N by 1){i => 
        Foreach(0 until 1 by 1){j =>
          argout := mux(j == 0, i, mem(i))
        }
        Foreach(32 by 1){j => mem(j) = j}
      }
      dram store mem
    }

    val got = getMem(dram)
    val gold = Array.tabulate(32){i => i}
    assert(gold == got)
    printArray(got, "Got")
    printArray(gold, "Gold")
    println(r"dummy: $argout")
    println(r"Pass: ${gold == got}")
  }
}

