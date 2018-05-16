package spatial.tests.feature.banking


import spatial.dsl._


@test class FIFOBanking extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


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
