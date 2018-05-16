package spatial.tests.feature.transfers


import spatial.dsl._


@test class TileLoadStoreSimple extends SpatialTest {
  override def runtimeArgs: Args = "16"

  val N = 192

  def simpleLoadStore[T:Num](srcHost: Array[T], value: T): Array[T] = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)
    val tileSize = 96 (96 -> 96)

    val srcFPGA = DRAM[T](N)
    val dstFPGA = DRAM[T](N)
    setMem(srcFPGA, srcHost)

    val size = ArgIn[Int]
    val x = ArgIn[T]
    setArg(x, value)
    setArg(size, N)
    Accel {
      val b1 = SRAM[T](tileSize)
      Sequential.Foreach(size by tileSize) { i =>

        b1 load srcFPGA(i::i+tileSize)

        val b2 = SRAM[T](tileSize)
        Foreach(tileSize by 1) { ii =>
          b2(ii) = b1(ii) * x
        }

        dstFPGA(i::i+tileSize) store b2
      }
    }
    getMem(dstFPGA)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = N
    val value = args(0).to[Int]

    val src = Array.tabulate[Int](arraySize) { i => i }
    val dst = simpleLoadStore(src, value)

    val gold = src.map { _ * value }

    println("Sent in: ")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("Got out: ")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (SimpleTileLoadStore)")
    assert(cksum)
  }
}