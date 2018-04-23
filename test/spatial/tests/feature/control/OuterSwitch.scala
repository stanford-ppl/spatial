package spatial.tests.feature.control


import spatial.dsl._


@test class OuterSwitch extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

   def main(args: Array[String]): Unit = {

    val in = ArgIn[Int]
    setArg(in, 20)

    val dram = DRAM[Int](32)

    Accel {
      val data = SRAM[Int](32)
      if (in.value <= 28) {
        Sequential.Foreach((in.value+4) by 1){ i => data(i) = i }
      }
      dram(0::32) store data
    }

    printArray(getMem(dram), "dram")
  }
}
