package spatial.tests.feature.host


import spatial.dsl._


@spatial class TransferArray extends SpatialTest {
  override def runtimeArgs: Args = "50"

  val N = 192
  type T = Int
  def memcpyViaFPGA(srcHost: Array[T]): Array[T] = {
    val fpgaMem = DRAM[Int](N)
    setMem(fpgaMem, srcHost)

    val y = ArgOut[Int]
    Accel { y := 10 }

    getMem(fpgaMem)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = N
    val c = args(0).to[Int]

    val src = Array.tabulate(arraySize){i => i*c }
    val dst = memcpyViaFPGA(src)
    printArray(src, "Source")
    printArray(dst, "Dest")
    assert(src == dst)
  }
}
