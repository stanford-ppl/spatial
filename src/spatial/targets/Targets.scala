package spatial.targets

object Targets {
  var targets: Set[HardwareTarget] = Set.empty

  def Default: HardwareTarget = xilinx.Zynq
}
