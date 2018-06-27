package spatial.tests.feature.transfers


import spatial.dsl._

@spatial class AXICommandSplit extends SpatialTest {
  override def runtimeArgs: Args = "12288"

  val rows = 4
  
  def onetilestore(srcHost: Array[Int], value: Int) = {
    val loadPar  = 16 (1 -> 1)
    val storePar = 16 (1 -> 1)
    val tileSize = 6144

    val N = ArgIn[Int]
    setArg(N, value)
    val src = DRAM[Int](rows,N)
    val dst = DRAM[Int](rows,N)
    setMem(src, srcHost)
    setMem(dst, Array.fill[Int](rows*value)(0))

    Accel {
      Sequential.Foreach(N.value by tileSize par 1) { i =>
        val b1 = SRAM[Int](rows,tileSize)
        b1 load src(0::rows, i::i+tileSize par loadPar)
        dst(0::rows, i::i+tileSize par storePar) store b1
      }
    }
    getMem(dst)
  }

  def main(args: Array[String]): Void = {
    val arraySize = args(0).to[Int]

    val src = Array.tabulate[Int](rows*arraySize) { i => i }
    val dst = onetilestore(src, arraySize)
    printArray(dst, "got ")

    val cksum = src.zip(dst){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (AXICommandSplit)")
    assert(cksum)
  }
}
