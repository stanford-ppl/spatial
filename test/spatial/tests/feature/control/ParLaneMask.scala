package spatial.tests.feature.control

import spatial.dsl._

@spatial class ParLaneMask extends SpatialTest {
  override def runtimeArgs: Args = "13"
  /*
    This app is for testing the valids that get passed to each child of a metapipe,
    and before this bug was caught in MD_Grid, the enable for all stages was computed
    based on the current counter value for stage 0
  */


  def main(args: Array[String]): Unit = {

    val x = ArgIn[Int] // Should NOT be multiple of 2
    val y = ArgOut[Int]
    val ymem = DRAM[Int](4,16)
    setArg(x, args(0).to[Int])

    Accel {
      val s = SRAM[Int](64)
      Foreach(64 by 1){i => s(i) = 0}
      Foreach(4 by 1 par 2){k =>
        val sum = Reduce(Reg[Int](0))(x by 1 par 4) {i =>
          val dummy = Reg[Int].buffer
          Pipe{dummy := i}
          Foreach(4 by 1){j => dummy := j}
          Pipe{dummy := i}
          dummy.value
        }{_+_}
        s(k) = sum
      }
      y := Reduce(Reg[Int](0))(64 by 1) { i => s(i) }{_+_}

      val outmem = SRAM[Int](4,16)
      Foreach(4 by 1 par 1){ k =>
        val s_accum = SRAM[Int](16)
        MemReduce(s_accum)(2 by 1, x by 1 par 2){(_,i)  =>
          val piece = SRAM[Int](16)
          Foreach(16 by 1){j => piece(j) = i}
          piece
        }{_+_}
        Foreach(16 by 1){ i => outmem(k,i) = s_accum(i)}
      }
      ymem store outmem

    }

    val gold = Array.tabulate(x){i => i}.reduce{_+_} * 4
    println("Wanted " + gold)
    println("Got " + getArg(y))

    val gold_matrix = (0::4,0::16){(_,_) => 2*Array.tabulate(x){i => i}.reduce{_+_} }
    printMatrix(getMatrix(ymem), "Mem:")
    printMatrix(gold_matrix, "Expected:")

    val cksum = gold == getArg(y) && gold_matrix.zip(getMatrix(ymem)){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (LaneMaskPar)")
    assert(cksum)
  }
}


