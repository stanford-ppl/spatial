package spatial.test

abstract class Testbench extends nova.test.NovaTestbench {
//  override val defaultArgs = Array("--vv", "--test", "--fpga", "zynq")
  override val defaultArgs = Array("--vv", "--test", "--sim", "--fpga", "zynq")
}
