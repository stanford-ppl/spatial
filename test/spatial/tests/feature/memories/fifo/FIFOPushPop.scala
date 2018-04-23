package spatial.tests.feature.memories.fifo


import spatial.dsl._


@test class FIFOPushPop extends SpatialTest {
  override def runtimeArgs: Args = "384"


  def fifopushpop(N: Int): Int = {
    val tileSize = 96 (96 -> 96)

    val size = ArgIn[Int]
    setArg(size, N)
    val acc = ArgOut[Int]

    Accel {
      val f1 = FIFO[Int](tileSize)
      val accum = Reg[Int](0)
      Reduce(accum)(size by tileSize){ iter =>
        Foreach(tileSize by 1){i => f1.enq(iter + i) }
        Reduce(0)(tileSize by 1){ i =>
          f1.deq()
        }{_+_}
      }{_+_}
      acc := accum
    }
    getArg(acc)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val gold = Array.tabulate(arraySize){ i => i }.reduce{_+_}
    val dst = fifopushpop(arraySize)

    println("gold: " + gold)
    println("dst: " + dst)

    val cksum = dst == gold
    println("PASS: " + cksum + " (FifoPushPop)")
    assert(cksum)
  }
}

@test class FIFOPushPop2 extends SpatialTest {
  override def runtimeArgs: Args = "384"

  def fifopushpop(N: Int): Int = {
    val tileSize = 16 (16 -> 16)

    val size = ArgIn[Int]
    setArg(size, N)
    val acc = ArgOut[Int]

    Accel {
      val f1 = FIFO[Int](tileSize)
      val accum = Reg[Int](0)
      Sequential.Reduce(accum)(size by tileSize){ iter =>
        Foreach(tileSize/2 by 1 par 2){i => f1.enq(iter + i) }
        Foreach(tileSize-1 until (tileSize/2)-1 by -1 par 2){i => f1.enq(iter + i) }
        Reduce(0)(tileSize by 1){ i =>
          f1.deq()
        }{_+_}
      }{_+_}
      acc := accum
    }
    getArg(acc)
  }


  def main(args: Array[String]): Unit = {
    val arraySize = args(0).to[Int]

    val gold = Array.tabulate(arraySize){ i => i }.reduce{_+_}
    val dst = fifopushpop(arraySize)

    println("gold: " + gold)
    println("dst: " + dst)

    val cksum = dst == gold
    println("PASS: " + cksum + " (FifoPushPop)")
  }
}
