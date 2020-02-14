package spatial.tests.feature.memories.sparsememories

import spatial.dsl._

@spatial class SparseMemories extends SpatialTest {
  override def runtimeArgs: Args = "32"
  //type T = FixPt[TRUE, _16, _16]
  type T = Int

  def main(args: Array[String]): Unit = {

    def func(i: Int): T = 2.to[T] * i.to[T]
    Accel {
      // Test dense read/write and RMW
      val s1 = SparseSRAM[T](128)
      Foreach(10 by 1) { _ =>
        Foreach(128 by 1 par 16) { i => s1(i) = i.to[T] }
        Foreach(128 by 1 par 16) { i => s1.RMW(i, func(i), "add", "TSO") }
        Foreach(128 by 1 par 16) { i => println(r"${s1(i)}") }
      }
      val s2 = SparseSRAM[T](128)
      val s3 = SparseSRAM[T](128)
      // I don't know what the following control structure is supposed to do but it is a vehicle for generating the IR we talked about
      val outerBarrier = Barrier[Token](5) // The type arg doesn't actually do anything right now
      Foreach(16 by 1 par 2) { i =>
        val midBarrier = Barrier[Token](1) // The type arg doesn't actually do anything right now
        Foreach(128 by 1 par 16) { j =>
          val innerBarrier = Barrier[Token](32) // The type arg doesn't actually do anything right now
          s2.barrierWrite(j, func(j), Seq(outerBarrier.push, midBarrier.push, innerBarrier.push))
          s3.barrierWrite(j, func(j), Seq(innerBarrier.pop, midBarrier.push))
          val r2 = s2.barrierRead(j, Seq(midBarrier.pop, outerBarrier.pop))
          println(r"$r2 ")
        }
      }
    }

    assert(true)
  }
}
