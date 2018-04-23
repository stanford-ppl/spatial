package spatial.tests.feature.memories.dram


import spatial.dsl._



@test class DRAMRuntime extends SpatialTest {
  override def runtimeArgs: Args = "0 1 2 3 4 5 6 7 8 9"


  def main(args: Array[String]): Unit = {
    val arr = args.map{ a => a.to[Int] }
    val x = DRAM[Int](arr.length)
    val N = ArgIn[Int]
    setArg(N, args.length)
    setMem(x, arr)
    val out = ArgOut[Int]
    Accel {
      out := Reduce(0)(N by 5) { i =>
        val sram = SRAM[Int](12)
        sram load x(i :: i + 5)
        Reduce(0)(5 by 1) { j => sram(j) } {_ + _}
      }{_+_}
    }
    println(getArg(out))
    assert(getArg(out) == (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9).to[Int])
  }
}
