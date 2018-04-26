package spatial.tests.feature.math

import spatial.dsl._
import spatial.lib._

@test class LowPrecisionRep extends SpatialTest { // Args N in multiple of 64
  override def runtimeArgs: Args = NoArgs

  type T = Float
  type B = Byte

  def main(args: Array[String]): Unit = {
    val data = Array.tabulate(1000){i => random[Float]( 256) - 128 }
    val N = data.length

    val dram = DRAM[T](N)
    setMem(dram, data)
    val dram_out   = DRAM[B](N)
    val sf = ArgOut[Float]

    Accel {
      val x = Reg[Float]
      ConvertTo8Bit(dram_out,x,dram,64)
      sf := x.value
    }

    val inputArray = getMem(dram)
    val outputArray = getMem(dram_out)
    val scalingFactor = getArg(sf)

    val maxGold = inputArray.reduce{(a,b) => max(abs(a),abs(b)) }
    val maxDelta = 2.0.to[T]*maxGold/127.to[T]
    val goldArray = inputArray.map{a => (a/maxDelta).to[B] }

    val matches = outputArray === goldArray
    println("result:   " + matches)

    println("Scaling Factor: " + scalingFactor)
    (0 until N).foreach{ i =>
      println("input: " + inputArray(i) + ", gold: " + goldArray(i) + ", actual: " + outputArray(i))
    }

    assert(matches)
  }
}