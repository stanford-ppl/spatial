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
      // Test token write/read
      val s2 = SparseSRAM[T](128)
      val s3 = SparseSRAM[T](128)
      Foreach(128 by 1 par 16) { i =>
        val token1 = s2.tokenWrite(i, func(i))
        val token2 = s3.tokenWrite(i, func(i))
        val r2 = s2.tokenRead(i, token1 && token2)
        println(r"$r2 ")
      }
    }

    assert(true)
  }
}
