package spatial.tests.feature.memories.reg


import spatial.dsl._


@test class StructReg extends SpatialTest {
  override def runtimeArgs: Args = "32"

  def main(args: Array[String]): Unit = {
    type Tup = Tup2[Int16, Int16]

    val in1 = ArgIn[Int16]
    val in2 = ArgIn[Int16]
    val out = ArgOut[Tup]
    setArg(in1, args(0).to[Int16])
    setArg(in2, args(1).to[Int16])

    Accel {
      val reg = Reg[Tup](pack(2.to[Int16],3.to[Int16]))
      val x = reg._1 + in1.value
      val y = reg._2 + in2.value
      println(x)
      println(y)
      out := pack(x,y)
    }

    assert(getArg(out)._1 == args(0).to[Int16] + 2)
    assert(getArg(out)._2 == args(1).to[Int16] + 3)
  }
}