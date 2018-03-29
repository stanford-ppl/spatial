package spatial.targets

object Targets {
  var targets: Set[HardwareTarget] = Set(
    xilinx.Zynq,
    xilinx.ZCU,
    xilinx.AWS_F1
  )

  def Default: HardwareTarget = xilinx.Zynq
}
