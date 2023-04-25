package spatial.tests.ee109
import spatial.dsl._
@spatial class Lab2Part4LUT extends SpatialTest {
  override def runtimeArgs = "0 0 0"
  def main(args: Array[String]): Unit = {
    type T = Int
    val M = 3
    val N = 3

    val in = ArgIn[T]
    val out = ArgOut[T]
    val i = ArgIn[T]
    val j = ArgIn[T]

    val input = args(0).to[T]
    val ind_i = args(1).to[T]
    val ind_j = args(2).to[T]

    setArg(in, input)
    setArg(i, ind_i)
    setArg(j, ind_j)

    Accel {
      // Your code here
      val lut = LUT[Int](3,3)(1, 2, 3, 4, 5, 6, 7, 8, 9)
      out := in.value + lut(i.value, j.value)
    }

    val result = getArg(out)
    val goldArray = Array.tabulate(M * N){ i => i + 1 }
    val gold = input + goldArray(i*N + j)
    val pass = gold == result
    println("PASS: " + pass + "(Lab2Part4LUT)")

    assert(gold == result)
  }
}