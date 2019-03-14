package spatial.tests.feature.control

import spatial.dsl._

@spatial class OuterSwitch extends SpatialTest {
  override def dseModelArgs: Args = "24 100"
  override def finalModelArgs: Args = "24 100"
  override def runtimeArgs: Args = "20" and "30"

  def main(args: Array[String]): Unit = {
    val n = args(0).to[Int]

    val in = ArgIn[Int]
    setArg(in, n)

    val dram = DRAM[Int](32)

    Accel {
      val data = SRAM[Int](32)
      Sequential.Foreach(32 by 1){i => data(i) = 0}
      if (in.value <= 28) {
        Sequential.Foreach((in.value+4) by 1){ i => data(i) = i }
      }
      dram(0::32) store data
    }

    printArray(getMem(dram), "dram")
    if (n > 28) assert(getMem(dram) == Array.fill(32){ 0.to[Int] })
    else        assert(getMem(dram) == Array.tabulate(32){i => if (i < n + 4) i else 0 })
  }
}
