package fringe.targets

import chisel3.Data
import fringe._
import fringe.utils.log2Up

trait DeviceTarget {
  type Reset = chisel3.core.Reset

  // This forces bigIP to always be lazy
  private var __bigIP: Option[BigIP] = None
  final def bigIP: BigIP = {
    if (__bigIP.isEmpty) __bigIP = Some(makeBigIP)
    __bigIP.get
  }
  def makeBigIP: BigIP

  val magPipelineDepth: Int = 1

  val fixmul_latency = 0.03125
  val fixdiv_latency = 0.03125
  val fixadd_latency = 0.1875
  val fixsub_latency = 0.625
  val fixmod_latency = 0.5
  val fixeql_latency = 1
  val sramload_latency = 0
  val sramstore_latency = 0

  val SramThreshold = 0 // Threshold between turning Mem1D into register array vs real memory
  val mux_latency = 1

  // TODO: What is this?
  lazy val delay_per_numIter: Double = List(
    fixsub_latency*32 + fixdiv_latency*32 + fixadd_latency*32,
    fixmul_latency*32 + fixdiv_latency*32 + fixadd_latency*32,
    fixsub_latency*32 + fixmod_latency*32 + fixeql_latency + mux_latency + fixadd_latency*32,
    fixmul_latency*32 + fixmod_latency*32 + fixeql_latency + mux_latency + fixadd_latency*32
  ).max

  // TODO: What is this?
  val addrWidth: Int = 32

  // TODO: What is this?
  val dataWidth: Int = 32

  // TODO: Number of words in the same stream
  val wordsPerStream: Int = 16

  // TODO: What is this?
  val external_w = 32

  // TODO: What is this?
  val external_v = 16

  // TODO: What is this?
  val target_w = 64

  val num_channels = 1

  def regFileAddrWidth(n: Int): Int = log2Up(n)

  // This is a hack to be able to register IO in the topInterface call
  var makeIO: Function[Data,Data] = _
  final def IO[T<:Data](io: T): T = makeIO(io).asInstanceOf[T]

  /** Creates the Top IO and Fringe modules for this target. Should be called only from within Top. */
  def topInterface(reset: Reset, accel: AbstractAccelTop): TopInterface
}
