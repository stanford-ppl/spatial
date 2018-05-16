package spatial.tests.syntax

import spatial.dsl._

@test class NamedTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends: Seq[Backend] = DISABLED // TODO: Just a syntax example for now

  def funcA(parent: Label, N: Int, sram: SRAM1[Int]): Unit = {
    Named(s"${parent}_FuncA").Foreach(0 until N){i => println(sram(i)) }
  }

  def funcB(): Unit = {
    val x = SRAM[Int](32)
    funcA("FuncB", 32, x)
  }

  def funcC(): Unit = {
    val x = SRAM[Int](64)
    funcA("FuncC", 64, x)
  }


  def main(args: Array[String]): Unit = {

    'X.Accel {
      val a = SRAM[Int](32)
      val b = Reg[Int](0)

      'A.Foreach(0 until 32){i => println(a(i)) }
      'A.Foreach(0 until 32){i => println(a(i)) }

      'B.Reduce(b)(0 until 32){i => a(i) }{_+_}
      'B.Reduce(b)(0 until 32){i => a(i) }{_+_}
      val sum = 'C.Fold(0)(0 until 32){i => a(i) }{_+_}

      'D.MemReduce(a)(0 until 32){i => a }{_+_}
      'D.MemReduce(a)(0 until 32){i => a }{_+_}
      'E.MemFold(a)(0 until 32){i => a }{_+_}
      'E.MemFold(a)(0 until 32){i => a }{_+_}

      'F.Pipe{ println(sum.value) }

      println(b.value)
      println(sum.value)

      funcB()
      funcC()
    }
  }
}
