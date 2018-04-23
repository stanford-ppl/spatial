package spatial.tests.feature.math


import spatial.dsl._


import scala.reflect.ClassTag

@test class FixBasics extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type Q16 = FixPt[TRUE,_16,_16]
  val N: scala.Int = 20

   def enumerate[T:Num:ClassTag]: scala.Array[T] = {
    val a = new scala.Array[T](N)
    Pipe {
      val x = random[T]
      val y = random[T]
      val z = random[T]
      a(0) = x
      a(1) = tanh(x)
      a(2) = exp(x)
      a(3) = x*y + z
      a(4) = x*z
      a(5) = x + z
      a(6) = x / z
      a(7) = x.to[Int].as[T]
      a(8) = x.to[Q16].as[T]
      a(9) = x.to[Float].as[T]
      a(10) = x.to[Half].as[T]
      a(11) = abs(x)
      a(12) = x % z
      a(13) = sqrt(x)
      a(15) = ln(x)
      a(16) = 1.to[T] / x
      a(17) = 1.to[T] / sqrt(y)
      a(18) = floor(x)
      a(19) = ceil(x)
    }
    a
  }
   def test[T:Num:ClassTag]: SRAM1[T] = {
    val sram = SRAM[T](N)
    val array = enumerate[T]
    Pipe {
      array.zipWithIndex.foreach{case (s,i) => Pipe { sram(i) = s } }
    }
    sram
  }

   def main(args: Array[String]): Unit = {
    // RNG
    val dramInt = DRAM[Int](N)
    val dramQ16 = DRAM[Q16](N)
    val dramHalf = DRAM[Half](N)
    val dramFloat = DRAM[Float](N)

    Accel {
      val sramInt = test[Int]
      val sramQ16 = test[Q16]
      val sramHalf = test[Half]
      val sramFloat = test[Float]

      dramInt store sramInt
      dramQ16 store sramQ16
      dramHalf store sramHalf
      dramFloat store sramFloat
    }

    printArray(getMem(dramInt), "Int")
    printArray(getMem(dramQ16), "Q16")
    printArray(getMem(dramHalf), "Half")
    printArray(getMem(dramInt), "Float")

  }
}
