package spatial.tests.feature.memories.reg

import spatial.dsl._

@spatial class StructReg extends SpatialTest {
  override def runtimeArgs: Args = "32, 8"

  def main(args: Array[String]): Unit = {
    type Tup = Tup2[Int16, Int16]

    val a = args(0).to[Int16]
    val b = args(1).to[Int16]
    val in1 = ArgIn[Int16]
    val in2 = ArgIn[Int16]
    val out = ArgOut[Tup]
    setArg(in1, a)
    setArg(in2, b)

    Accel {
      val reg = Reg[Tup](pack(2.to[Int16],3.to[Int16]))
      val x = reg._1 + in1.value
      val y = reg._2 + in2.value
      println(x)
      println(y)
      out := pack(x,y)
    }

    println(r"_1 = ${getArg(out)._1}, expected ${a + 2}")
    println(r"_2 = ${getArg(out)._2}, expected ${b + 3}")

    assert(getArg(out)._1 == a + 2)
    assert(getArg(out)._2 == b + 3)
  }
}