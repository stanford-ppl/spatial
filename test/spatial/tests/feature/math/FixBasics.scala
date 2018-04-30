package spatial.tests.feature.math

import spatial.dsl._

import scala.reflect.ClassTag

@test class FixBasics extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type Q16 = FixPt[TRUE,_16,_16]
  val N: scala.Int = 20

  def enumerate[T:Num:ClassTag](x: T, y: T, z: T): scala.Array[T] = {
    val a = scala.Array.fill(N){ 0.to[T] }
    a(0) = x
    a(1) = 0 /*tanh(x)*/
    a(2) = 0 /*exp(x)*/
    a(3) = x*y + z
    a(4) = x*z
    a(5) = x + z
    a(6) = x / z
    a(7) = x.to[Int].as[T]
    a(8) = x.to[Q16].as[T]
    a(9) = 0 /*x.to[Float].as[T]*/
    a(10) = 0 /*x.to[Half].as[T]*/
    a(11) = abs(x)
    a(12) = x % z
    a(13) = 0 /*sqrt(x)*/
    a(15) = 0 /*ln(x)*/
    a(16) = 1.to[T] / x
    a(17) = 0 /*1.to[T] / sqrt(y)*/
    a(18) = floor(x)
    a(19) = ceil(x)
    a
  }
   def test[T:Num:ClassTag](xOut: Reg[T], yOut: Reg[T], zOut: Reg[T]): SRAM1[T] = {
    val sram = SRAM[T](N)
    val x = random[T]
    val y = random[T]
    val z = random[T]
    val array = enumerate[T](x, y, z)
    Pipe {
      array.zipWithIndex.foreach{case (s,i) => Pipe { sram(i) = s } }
      xOut := x
      yOut := y
      zOut := z      
    }
    sram
  }

   def main(args: Array[String]): Unit = {
    // RNG
    val dramInt = DRAM[Int](N)
    val dramQ16 = DRAM[Q16](N)
    // val dramHalf = DRAM[Half](N)
    // val dramFloat = DRAM[Float](N)
    val xInt = ArgOut[Int]
    val yInt = ArgOut[Int]
    val zInt = ArgOut[Int]
    val xQ16 = ArgOut[Q16]
    val yQ16 = ArgOut[Q16]
    val zQ16 = ArgOut[Q16]
    // val xHalf = ArgOut[Half]
    // val yHalf = ArgOut[Half]
    // val zHalf = ArgOut[Half]
    // val xFloat = ArgOut[Float]
    // val yFloat = ArgOut[Float]
    // val zFloat = ArgOut[Float]

    Accel {
      val sramInt = test[Int](xInt, yInt, zInt)
      val sramQ16 = test[Q16](xQ16, yQ16, zQ16)
      // val sramHalf = test[Half](xHalf, yHalf, zHalf)
      // val sramFloat = test[Float](xFloat, yFloat, zFloat)

      dramInt store sramInt
      dramQ16 store sramQ16
      // dramHalf store sramHalf
      // dramFloat store sramFloat
    }

    println(r"Int Args: ${getArg(xInt)}, ${getArg(yInt)}, ${getArg(zInt)}")
    printArray(getMem(dramInt), "Int")
    println(r"Int Args: ${getArg(xQ16)}, ${getArg(yQ16)}, ${getArg(zQ16)}")
    printArray(getMem(dramQ16), "Q16")
    // printArray(getMem(dramHalf), "Half")
    // printArray(getMem(dramFloat), "Float")

    assert(getMem(dramInt) == enumerate[Int](getArg(xInt), getArg(yInt), getArg(zInt)))
    assert(getMem(dramQ16) == enumerate[Q16](getArg(xQ16), getArg(yQ16), getArg(zQ16)))
    // assert(getMem(dramHalf) == enumerate[Half](getArg(xHalf), getArg(yHalf), getArg(zHalf)))
    // assert(getMem(dramFloat) == enumerate[Float](getArg(xFloat), getArg(yFloat), getArg(zFloat)))
  }
}
