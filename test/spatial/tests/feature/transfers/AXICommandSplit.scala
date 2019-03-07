package spatial.tests.feature.transfers


import spatial.dsl._

@spatial class AXICommandSplit extends SpatialTest {
  override def dseModelArgs: Args = "12288"
  override def finalModelArgs: Args = "12288"
  override def runtimeArgs: Args = "12288"

  val rows = 4
  

  def main(args: Array[String]): Void = {
    val arraySize = args(0).to[Int]

    val srcHost = Array.tabulate[Int](rows*arraySize) { i => i }
    val loadPar  = 16 (1 -> 1)
    val storePar = 16 (1 -> 1)
    val tileSize = 6144

    val N = ArgIn[Int]
    setArg(N, arraySize)
    val src = DRAM[Int](rows,N)
    val dst = DRAM[Int](rows,N)
    val check = ArgOut[Int]
    setMem(src, srcHost)
    setMem(dst, Array.fill[Int](rows*arraySize)(0))

    Accel {
      Sequential.Foreach(N.value by tileSize par 1) { i =>
        val b1 = SRAM[Int](rows,tileSize)
        b1 load src(0::rows, i::i+tileSize par loadPar)
        check := b1(3,6142)
        dst(0::rows, i::i+tileSize par storePar) store b1
      }
    }
    val got = getMem(dst)

    printArray(got, "got ")
    println(r"check @ 3,6142 = ${getArg(check)}")

    val cksum = srcHost.zip(got){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (AXICommandSplit)")
    assert(cksum)
  }
}
