package fringe
import java.io.{File, PrintWriter}
import fringe.bigIP.BigIP

sealed trait ChannelAssignment
object AllToOne extends ChannelAssignment          // Assign all drams to channel 0
object BasicRoundRobin extends ChannelAssignment   // Assign dram i to channel i % numChannels
object ColoredRoundRobin extends ChannelAssignment // Assign dram i to color % numChannels, where color is computed in spatial
object AdvancedColored extends ChannelAssignment   // Assign dram i to color, based on spatials advanced allocation rules that account for transfer intensity (TODO)

// Some global constants
object FringeGlobals {
  // BigIP handle
  private var _bigIP: BigIP = _
  def bigIP = _bigIP
  def bigIP_= (value: BigIP): Unit = _bigIP = value

  // DRAM interface pipeline depth
  private var _magPipelineDepth: Int = 1
  def magPipelineDepth = _magPipelineDepth
  def magPipelineDepth_= (value: Int): Unit = _magPipelineDepth = value

  private var _target: String = ""
  def target = _target
  def target_= (value: String): Unit = {
    bigIP = value match {
      case "zynq" | "zcu" => new fringeZynq.bigIP.BigIPZynq()
      case "aws"          => new fringeAWS.bigIP.BigIPAWS()
      case "de1soc"       => new fringeDE1SoC.bigIP.BigIPDE1SoC()
      case "asic"         => new fringeASIC.bigIP.BigIPASIC()
      case _              => new fringe.bigIP.BigIPSim()
    }

    magPipelineDepth = value match {
      case "zynq" | "zcu" => 0
      case _ => 1
    }

    _target = value
  }

  private var _enableDebugRegs: Boolean = true
  def enableDebugRegs = _enableDebugRegs
  def enableDebugRegs_= (value: Boolean): Unit = _enableDebugRegs = value

  private var _channelAssignment: ChannelAssignment = ColoredRoundRobin
  def channelAssignment = _channelAssignment
  def channelAssignment_= (value: ChannelAssignment): Unit = _channelAssignment = value

  // tclScript
  private var _tclScript: PrintWriter = {
    val pw = new PrintWriter(new File("bigIP.tcl"))
    pw.flush
    pw
  }

  def tclScript = _tclScript
  def tclScript_= (value: PrintWriter): Unit = _tclScript = value


}
