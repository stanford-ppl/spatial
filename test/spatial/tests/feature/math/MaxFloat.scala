package spatial.tests.feature.math

import spatial.dsl._

@test class MaxFloat extends SpatialTest {
  override def runtimeArgs: Args = "128"
  type T = Float


  def main(args: Array[String]): Unit = {
    val tileSize = define("tileSize", 64);
    val parN = define("par", 1);
    val N = args(0).to[Int]
    val length = ArgIn[Int]

    Console.out.println(s"tileSize: $tileSize, par: $parN")
    setArg(length, N)

    val dataX  = Array.tabulate[T](N){i => random[T] }
    val dataY  = Array.tabulate[T](N){i => random[T] }
    val dramX  = DRAM[T](length)
    val dramY  = DRAM[T](length)
    val outram = DRAM[T](length)
    setMem(dramX, dataX)
    setMem(dramY, dataY)

    Accel {

      'Max.Pipe.Foreach (0 until length by tileSize par parN) { i=>
        val sramX    = SRAM[T](tileSize)
        val sramY    = SRAM[T](tileSize)
        val sramout  = SRAM[T](tileSize)
        sramX load dramX(i::i+tileSize)
        sramY load dramY(i::i+tileSize)
        Foreach (0 until tileSize) { j =>
          sramout(j) = max(sramX(j),sramY(j))
        }
        outram(i::i+tileSize) store sramout
      }
    }

    val result = getMem(outram)
    printArray(dataX, " Input X: ")
    printArray(dataY, " Input Y: ")
    printArray(result, "Output : ")

    (0 until length) foreach { i =>
      val expect = max(dataX(i), dataY(i))
      assert(result(i) == expect, "Mismatch i " + i + " X:" + dataX(i) + " Y: " + dataY(i) + " expected: " + expect + " got: " + result(i))
    }
  }
}
