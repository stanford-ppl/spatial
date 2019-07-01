package fringe

import chisel3._

abstract class TopInterface extends Bundle {
  // Host scalar interface
  var raddr = Input(UInt(1.W))
  var wen  = Input(Bool())
  var waddr = Input(UInt(1.W))
  var wdata = Input(Bits(1.W))
  var rdata = Output(Bits(1.W))
  // val is_enabled = Output(Bool())
}

/** Top module including Fringe and Accel
  * @param targetName The name of the target architecture
  * @param accelGen Delayed creation of AccelTop
  */
class Top(targetName: String, accelGen: () => AbstractAccelTop) extends Module {

  globals.target = targetName match {
    case "verilator" => new targets.verilator.Verilator
    case "vcs"  | "VCS"       => new targets.vcs.VCS
    case "xsim"      => new targets.xsim.XSim
    case "aws"  | "AWS_F1"     => new targets.aws.AWS_F1
    case "cxp"  | "CXP"     => new targets.cxp.CXP
    case "aws-sim"   => new targets.aws.AWS_Sim
    case "zynq" | "Zynq"      => new targets.zynq.Zynq
    case "zedboard" | "ZedBoard"      => new targets.zedboard.ZedBoard
    case "zcu"  | "ZCU"       => new targets.zcu.ZCU
    case "arria10" | "Arria10"   => new targets.arria10.Arria10
    case "asic" | "ASIC"     => new targets.asic.ASIC
    case "kcu1500" | "KCU1500"     => new targets.kcu1500.KCU1500
    case _           => throw new Exception(s"Unknown target '$targetName'")
  }

  globals.target.makeIO = { x: Data => IO(x) }
  val accel = accelGen()
  accel.io <> DontCare
  val io = globals.target.topInterface(reset, accel)
  // Accel

  // TBD: Accel <-> Fringe Memory connections
//  for (i <- 0 until numMemoryStreams) {
//    fringe.io.memStreams(i).cmd.bits := accel.io.memStreams(i).cmd.bits
//    fringe.io.memStreams(i).cmd.valid := accel.io.memStreams(i).cmd.valid
//    fringe.io.memStreams(i).wdata.bits := accel.io.memStreams(i).wdata.bits
//    fringe.io.memStreams(i).wdata.valid := accel.io.memStreams(i).wdata.valid
//    accel.io.memStreams(i).rdata.bits := fringe.io.memStreams(i).rdata.bits
//    accel.io.memStreams(i).rdata.valid := fringe.io.memStreams(i).rdata.valid
//  }

}
