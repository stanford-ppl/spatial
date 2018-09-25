package spatial.tests.feature.math

import spatial.dsl._

import scala.reflect.ClassTag

@spatial class FixBasics extends SpatialTest {
  type Q16 = FixPt[TRUE,_16,_16]
  val N: scala.Int = 20

  def enumerate[T:Num:ClassTag](x: T, y: T, z: T): scala.Array[T] = {
    val a = scala.Array.fill(N){ 0.to[T] }
    a(0) = x
    a(1) = 0 /*atan(x)*/
    a(2) = 0 /*exp(x)*/
    a(3) = x*y + z
    a(4) = x*z
    a(5) = x + z
    a(6) = x / z
    a(7) = x.toSaturating[Int].as[T]
    a(8) = x.toSaturating[Q16].as[T]
    a(9) = x.to[Float].to[T]
    a(10) = x.to[Half].to[T]
    a(11) = abs(x)
    a(12) = x % z
    a(13) = sqrt(x)
    a(15) = 0 /*ln(x)*/
    a(16) = 1.to[T] / x
    a(17) = 1.to[T] / sqrt(y)
    a(18) = floor(x)
    a(19) = ceil(x)
    a
  }
   def test[T:Num:ClassTag](xOut: Reg[T], yOut: Reg[T], zOut: Reg[T]): SRAM1[T] = {
    val sram = SRAM[T](N)
    val x = random[T](200.to[T]) // Capped because f > 0 FixPt arithmetic not truly implemented in cpp
    val y = random[T](200.to[T])
    val z = random[T](200.to[T])
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
    val xInt = ArgOut[Int]
    val yInt = ArgOut[Int]
    val zInt = ArgOut[Int]
    val xQ16 = ArgOut[Q16]
    val yQ16 = ArgOut[Q16]
    val zQ16 = ArgOut[Q16]

    Accel {
      val sramInt = test[Int](xInt, yInt, zInt)
      val sramQ16 = test[Q16](xQ16, yQ16, zQ16)

      dramInt store sramInt
      dramQ16 store sramQ16
    }

    val goldInt = enumerate[Int](getArg(xInt), getArg(yInt), getArg(zInt))
    println(r"Int Args: ${getArg(xInt)}, ${getArg(yInt)}, ${getArg(zInt)}")
    printArray(getMem(dramInt), "Int")
    printArray[Int](Array(goldInt:_*), "Gold")
    println("")

    val goldQ = enumerate[Q16](getArg(xQ16), getArg(yQ16), getArg(zQ16))
    println(r"Int Args: ${getArg(xQ16)}, ${getArg(yQ16)}, ${getArg(zQ16)}")
    printArray(getMem(dramQ16), "Q16")
    printArray[Q16](Array(goldQ:_*), "Gold")


    println(s"Big margin because vcs simulation of sqrt is sketchy")
    val cksumInt = Array[Int](goldInt:_*).zip(getMem(dramInt)){case(a,b) => abs(a-b) < 3}.reduce{_&_}
    val cksumQ   = Array[Q16](goldQ:_*).zip(getMem(dramQ16)){case(a,b) => abs(a-b) < 2.toUnchecked[Q16]}.reduce{_&_}
    println(r"Int: $cksumInt, Q: $cksumQ")
    assert(cksumInt)
    assert(cksumQ)
  }
}
