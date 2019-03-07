package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class TileLoadStoreSimple extends SpatialTest {
  override def dseModelArgs: Args = "192"
  override def finalModelArgs: Args = "192"
  override def runtimeArgs: Args = "16 192"

  def simpleLoadStore[T:Num](srcHost: Array[T], value: T): Array[T] = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)
    val tileSize = 96 (96 -> 96)

    val N = args(1).to[Int]
    val size = ArgIn[Int]
    setArg(size, N)
    val srcFPGA = DRAM[T](size)
    val dstFPGA = DRAM[T](size)
    setMem(srcFPGA, srcHost)

    val x = ArgIn[T]
    setArg(x, value)
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
    val arraySize = args(1).to[Int]
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

@spatial class PartialTileLoadStore extends SpatialTest {
  override def runtimeArgs: Args = "2"


  def simpleLoadStore[T:Num](srcHost: spatial.dsl.Matrix[T], value: T) = {
    val loadPar  = 1 (1 -> 1)
    val storePar = 1 (1 -> 1)

    val srcFPGA = DRAM[T](16,16)
    val dstFPGA = DRAM[T](16,16)
    setMem(srcFPGA, srcHost)

    val x = ArgIn[T]
    setArg(x, value)
    Accel {
      val b1 = SRAM[T](16, 16)
      Foreach(16 by 1 par 1) { i =>
        b1(i::i+1, 0::16) load srcFPGA(i::i+1, 0::16 par 1)
      }
      val b2 = SRAM[T](16,16)
      Foreach(16 by 1, 16 by 1 par 4) { (i,j) =>
        b2(i,j) = b1(i,j) * x
      }
      Foreach(16 by 1 par 1) {i => 
        dstFPGA(i::i+1, 0::16 par 1) store b2(i::i+1, 0::16)
      }
    }
    getMatrix(dstFPGA)
  }

  def main(args: Array[String]): Void = {
    val value = args(0).to[Int]

    val src = (0::16, 0::16) {(i,j) => i+j }
    val dst = simpleLoadStore(src, value)

    val gold = src.map { _ * value }

    printMatrix(gold, "Gold")
    printMatrix(dst, "got")

    val cksum = dst == gold
    println("PASS: " + cksum + " (PartialTileLoadStore)")
    assert(cksum)
  }
}
