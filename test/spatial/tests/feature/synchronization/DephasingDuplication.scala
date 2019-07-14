package spatial.tests.feature.synchronization

object Helper{
  def contains(a: Option[String], b: String): Boolean = {a.getOrElse("").indexOf(b) != -1}
}

@spatial class DephasingDuplication extends SpatialTest {
  override def dseModelArgs: Args = "16"
  override def finalModelArgs: Args = "16"


  // Inspired by MD_Grid
  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](4,16)
    val result = DRAM[Int](4)
    val out = ArgOut[Int]
    val data = (0::4,0::16){(i,j) => i*16 + j}
    val in = ArgIn[Int]
    setArg(in, 3)
    setMem(dram, data)

    Accel {
      val sram = SRAM[Int](4,16).hierarchical.noduplicate // Only try to bank hierarchically to expose bug #123
      sram load dram
      out := 'LOOP1.Reduce(Reg[Int])(4 by 1 par 2){i => 
        // val max = if (in.value < 10) 16 else 8 // Should always choose 16, but looks random to compiler (NOT TRUE AS OF 7/12/2019 BECAUSE CONDITION IS LANE-INVARIANT)
        // Reduce(Reg[Int])(max by 1 par 2){j =>  // j should dephase relative to different lanes of LOOP1 since this is variable counter (NOT TRUE AS OF 7/12/2019 BECAUSE OUTERMOST ITER OF MOP CHILD IS ALWAYS SYNCHRONIZED)
        val max = if (in.value + i < 10) 16 else 8 // Should always choose 16, but looks random to compiler
        Reduce(Reg[Int])(2 by 1, max by 1 par 2){(k,j) =>  // j should dephase relative to different lanes of LOOP1 since this is variable counter
          mux(k == 0, sram(i,j), 0)
        }{_+_}
      }{_+_}

    }

    val gold = data.flatten.reduce{_+_}
    val got = getArg(out)
    println(r"gold $gold =?= $out out")
    assert(out == gold)
  }

  override def checkIR(block: Block[_]): Result = {
    import argon._
    import spatial.metadata.memory._
    import spatial.node._

    // We should have 2 dups of sram
    val srams = block.nestedStms.collect{case Op(sram:SRAMNew[_,_]) => sram }
    if (srams.size > 0) assert (srams.size == 2)

    super.checkIR(block)
  }

}
