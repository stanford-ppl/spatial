package spatial.tests.compiler

import spatial.dsl._

@spatial class CtrCopyChunking extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val N = 64
    val out = ArgOut[Int]
    Accel {
      val regs = List.tabulate(N){i => Reg[Int](0)}
      Stream.Foreach(10 by 1){i => 
        regs.zipWithIndex.foreach{case(r,i) => Pipe{r := i}}
      }
      out := regs.map{_.value}.reduce{_+_}
    }

    val gold = List.tabulate(N){i => i}.reduce{_+_}
    assert(checkGold[Int](out, gold))
  }

}
