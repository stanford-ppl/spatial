package spatial

package object targets {
  lazy val fpgas: Set[HardwareTarget] = Set(
    xilinx.Zynq,
    xilinx.ZCU,
    xilinx.AWS_F1
  )

  def Default: HardwareTarget = xilinx.Zynq

  lazy val AWS_F1 = xilinx.AWS_F1
  lazy val ZCU = xilinx.ZCU
  lazy val Zynq = xilinx.Zynq
  lazy val Plasticine = plasticine.Plasticine

  lazy val all: Set[HardwareTarget] = fpgas + Plasticine
}
