package spatial.tests.feature.banking


import spatial.dsl._


@test class RegCoalescing extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]
    Accel {
      Foreach(16 by 1) {i =>
        val reg = Reg[Int]
        Pipe { reg := i }
        Pipe { out1 := reg.value }
        Pipe { out2 := reg.value }
      }
    }
  }
}
