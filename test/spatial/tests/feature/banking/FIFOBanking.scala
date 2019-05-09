package spatial.tests.feature.banking

import spatial.dsl._

@spatial class FIFOBanking extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val out = ArgOut[Int]

    Accel {
      val fifo = FIFO[Int](32)

      Foreach(16 by 1) {i =>
        Foreach(8 par 8){j => fifo.enq(j) }
        Foreach(8 par 1){j => out := fifo.deq() }
      }
    }

    assert(getArg(out) == 7.to[Int])
  }
}

@spatial class FIFOBanking2 extends SpatialTest {
  override def runtimeArgs: Args = "160"

  val N =  32

  def main(args: Array[String]): Unit = {

    val argOut = ArgOut[Int]

    Accel {
      val fifo = FIFO[Int](5)
      Foreach(N par 2){ i => 
        fifo.enq(i)
      }
      Reduce(argOut)(N par 3) { i =>
        fifo.deq
      } { _ + _ }
    }

    getArg(argOut)

    assert(true)
  }
}
