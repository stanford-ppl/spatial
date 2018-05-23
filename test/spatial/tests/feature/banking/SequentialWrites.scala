package spatial.tests.feature.banking

import spatial.dsl._

@test class SequentialWrites extends SpatialTest {
  override def runtimeArgs: Args = "32"

  val tileSize = 96
  val N = 5

  def sequentialwrites[A:Num](srcData: Array[A], x: A): Array[A] = {
    val T = param(tileSize)
    val P = param(4)
    val src = DRAM[A](T)
    val dst = DRAM[A](T)
    val xx = ArgIn[A]
    setArg(xx, x)
    setMem(src, srcData)
    Accel {
      val in = SRAM[A](T)
      in load src(0::T)

      MemFold(in)(N by 1){ i =>
        val d = SRAM[A](T)
        Foreach(T by 1 par P){ i => d(i) = xx.value + i.to[A] }
        d
      }{_+_}

      dst(0::T) store in
    }
    getMem(dst)
  }


  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int]
    val srcData = Array.tabulate(tileSize){ i => i }

    val result = sequentialwrites(srcData, x)

    val first = x*N
    val diff = N+1
    val gold = Array.tabulate(tileSize) { i => first + i*diff}

    printArray(gold, "gold: ")
    printArray(result, "result: ")
    val cksum = result.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum  + " (SequentialWrites)")
    assert(cksum)
  }
}
