package spatial.tests.feature.math

import spatial.dsl._



@test class Float32ToInt8 extends SpatialTest { // Args N in multiple of 64
  override def runtimeArgs: Args = NoArgs

  type T = Float
  type B = Byte


  def main(args: Array[String]): Unit = {
    val size = 64

    val data = loadCSV1D[Float]("/import/ch-pa-fpga1/pranjalib/python/float100.csv")
    val N = data.length

    //val data  = Array.tabulate(N){ i => if (i % 3 == 1) random[T](1024) else -1*random[T](1024) }
    val dram = DRAM[T](N)
    setMem(dram, data)

    val dram_out   = DRAM[B](N)

    Accel {
      val sram   = SRAM[T](size)
      val sram_out = SRAM[B](size)
      val maxo   = Reg[T]
      val delta = Reg[T]

      Reduce(maxo)(N by size){ii =>
        sram load dram(ii::ii+size)
        val maxi = Reg[T]
        Reduce(maxi)(0 until size){jj =>
          abs(sram(jj))
        }{(a,b) => max(a,b)}
      }{(a,b) => max(a,b)}

      Pipe {
        delta := 2.to[T]*(maxo)/127.to[T]   // TBD: Statistical Rounding not done. Instead we are doing Float2Fix
      }

      Foreach(N by size) { ii =>
        sram load dram(ii::ii+size)
        Foreach(0 until size) { jj =>
          sram_out(jj) = (sram(jj)/delta).to[B]
        }
        dram_out(ii::ii+size) store sram_out
      }
    }

    val inputArray = getMem(dram)
    val outputArray = getMem(dram_out)

    val maxGold = inputArray.reduce{(a,b) => max(abs(a),abs(b)) }
    val maxDelta = 2.0.to[T]*maxGold/127.to[T]
    val goldArray = inputArray.map{a => (a/maxDelta).to[B] }

    assert(outputArray === goldArray)

    (0 until N).foreach{ i =>
      println("input: " + inputArray(i) + ", gold: " + goldArray(i) + ", actual: " + outputArray(i))
    }

  }
}
