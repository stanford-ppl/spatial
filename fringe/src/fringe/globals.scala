package fringe
import java.io.{File, PrintWriter}

import fringe.targets.DeviceTarget
import fringe.templates.axi4.{AXI4BundleParameters, AXI4StreamParameters}

/** Fringe global constants. **/
object globals {
  /** Name of target. */
  var target: DeviceTarget = _

  /** Target-specific BigIP handle. */
  def bigIP: BigIP = target.bigIP

  /** DRAM interface pipeline depth. */
  def magPipelineDepth: Int = target.magPipelineDepth

  def ADDR_WIDTH: Int = target.addrWidth
  def DATA_WIDTH: Int = target.dataWidth
  def WORDS_PER_STREAM: Int = target.wordsPerStream
  def EXTERNAL_W: Int = target.external_w
  def EXTERNAL_V: Int = target.external_v
  def TARGET_W: Int = target.target_w
  def NUM_CHANNELS: Int = target.num_channels

  var retime = false
  var perpetual = false
  var enableModular: Boolean = true
  var enableVerbose: Boolean = false
  var enableDebugRegs: Boolean = true
  var channelAssignment: ChannelAssignment = AllToOne

  /** TCL script generator. */
  private var _tclScript: PrintWriter = {
    val pw = new PrintWriter(new File("bigIP.tcl"))
    pw.flush()
    pw
  }
  def tclScript: PrintWriter = _tclScript
  def tclScript_=(value: PrintWriter): Unit = _tclScript = value


  var regression_testing: String = scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0")

  // Top parameters
  // These are set by the generated Instantiator class
  var numArgIns: Int = 1      // Number of ArgIn registers
  var numArgOuts: Int = 1     // Number of ArgOut registers
  var numArgIOs: Int = 0      // Number of HostIO registers
  var numArgInstrs: Int = 0   // TODO: What is this?
  var argOutLoopbacksMap: Map[Int,Int] = Map.empty // TODO: What is this?

  var loadStreamInfo: List[StreamParInfo] = Nil
  var storeStreamInfo: List[StreamParInfo] = Nil
  var gatherStreamInfo: List[StreamParInfo] = Nil
  var scatterStreamInfo: List[StreamParInfo] = Nil
  var axiStreamInsInfo: List[AXI4StreamParameters] = List(AXI4StreamParameters(64,8,64))
  var axiStreamOutsInfo: List[AXI4StreamParameters] = List(AXI4StreamParameters(64,8,64))

  var numAllocators: Int = 0

  def LOAD_STREAMS: List[StreamParInfo] = if (loadStreamInfo.isEmpty) List(StreamParInfo(DATA_WIDTH, WORDS_PER_STREAM, 0)) else loadStreamInfo
  def STORE_STREAMS: List[StreamParInfo] = if (storeStreamInfo.isEmpty) List(StreamParInfo(DATA_WIDTH, WORDS_PER_STREAM, 0)) else storeStreamInfo
  def GATHER_STREAMS: List[StreamParInfo] = if (gatherStreamInfo.isEmpty) List(StreamParInfo(DATA_WIDTH, WORDS_PER_STREAM, 0)) else gatherStreamInfo
  def SCATTER_STREAMS: List[StreamParInfo] = if (scatterStreamInfo.isEmpty) List(StreamParInfo(DATA_WIDTH, WORDS_PER_STREAM, 0)) else scatterStreamInfo

  def AXI_STREAMS_IN: List[AXI4StreamParameters] = if (axiStreamInsInfo.isEmpty) List(AXI4StreamParameters(256,8,32)) else axiStreamInsInfo
  def AXI_STREAMS_OUT: List[AXI4StreamParameters] = if (axiStreamOutsInfo.isEmpty) List(AXI4StreamParameters(256,8,32)) else axiStreamOutsInfo

  def NUM_LOAD_STREAMS: Int = LOAD_STREAMS.size
  def NUM_STORE_STREAMS: Int = STORE_STREAMS.size

  def NUM_ARG_INS: Int = numArgIns
  def NUM_ARG_OUTS: Int = numArgOuts
  def NUM_ARG_IOS: Int = numArgIOs
  def NUM_ARG_LOOPS: Int = argOutLoopbacksMap.size max 1
  def NUM_ARGS: Int = numArgIns + numArgOuts
  def NUM_STREAMS: Int = LOAD_STREAMS.size + STORE_STREAMS.size
}
