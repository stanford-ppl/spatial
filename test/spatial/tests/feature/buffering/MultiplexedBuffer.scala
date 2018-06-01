package spatial.tests.feature.buffering

import spatial.dsl._

@test class MultiplexedBuffer extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    Accel {
      val a = SRAM[Int](32)

      'A.MemReduce(a)(0 until 32){i => a }{_+_}

      'B.MemReduce(a)(0 until 32){i => a }{_+_}
    }
  }
}

