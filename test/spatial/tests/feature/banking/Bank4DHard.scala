package spatial.tests.feature.banking

import spatial.dsl._

@spatial class Bank4DHard extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  // Not really "hard", but used to keep ExhaustiveBanking churning for a crazy long time
  val I = 16; val R = 3; val C = 3; val O = 16
  val PI = 2; val PR = 3;val PC= 3; val PO = 2

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](I,R,C,O)
    val data = (0::I,0::R,0::C,0::O){(i,j,k,l) => (i+j+k+l) % 5}
    setMem(dram,data)
    val out = ArgOut[Int]
    Accel {
      val x = SRAM[Int](I,R,C,O)
      x load dram

      out := Reduce(Reg[Int])(0 until I par PI, 0 until R par PR, 0 until C par PC, 0 until O par PO){case List(i,r,c,o) =>
        x(i,r,c,o)
      }{_+_}
    }

    val gold = data.reduce{_+_}
    val got = getArg(out)
    println(r"Got $got, expected $gold")
    println(r"PASS: ${got == gold}")
    assert(got == gold)
  }
}

