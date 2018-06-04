package spatial.tests.feature.transfers


import spatial.dsl._

@test class AXICommandSplit extends SpatialTest { // Regression (Unit) // Args: 100
  override def runtimeArgs: Args = "100"


  def onetileload(srcHost: Array[Int], value: Int) = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)
    val tileSize = 6144


    val N = ArgIn[Int]
    setArg(N, value)
    val srcFPGA = DRAM[Int](N)
    setMem(srcFPGA, srcHost)
    val out = ArgOut[Int]
    Accel {
      Sequential.Foreach(N.value by tileSize par 1) { i =>
        val b1 = SRAM[Int](tileSize)

        b1 load srcFPGA(i::i+tileSize par 1)

        val acc = Reg[Int]
        Foreach(tileSize by 1 par 1) { ii =>
          acc := b1(ii)
        }

        out := acc
      }
    }
    getArg(out)
  }

  def main(args: Array[String]): Void = {
    val arraySize = args(0).to[Int]

    val src = Array.tabulate[Int](arraySize) { i => i }
    val dst = onetileload(src, arraySize)
    println("got " + dst)

    val cksum = dst == src(arraySize-1)
    assert(cksum)
    println("PASS: " + cksum + " (OneTileLoad)")
  }
}
