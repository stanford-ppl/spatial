package spatial

package object targets {
  lazy val fpgas: Set[HardwareTarget] = Set(
    xilinx.Zynq,
    xilinx.ZCU,
    xilinx.ZedBoard,
    xilinx.AWS_F1,
    xilinx.KCU1500,
    euresys.CXP,
    altera.DE1,
    altera.Arria10,
    generic.VCS
  )

  def Default: HardwareTarget = xilinx.Zynq

  lazy val AWS_F1 = xilinx.AWS_F1
  lazy val KCU1500 = xilinx.KCU1500
  lazy val ZCU = xilinx.ZCU
  lazy val Zynq = xilinx.Zynq
  lazy val ZedBoard = xilinx.ZedBoard
  lazy val CXP = euresys.CXP
  lazy val VCS = generic.VCS
  lazy val DE1 = altera.DE1
  lazy val Arria10 = altera.Arria10
  lazy val ASIC = generic.ASIC
  lazy val Plasticine = plasticine.Plasticine

  lazy val all: Set[HardwareTarget] = fpgas + Plasticine
}
