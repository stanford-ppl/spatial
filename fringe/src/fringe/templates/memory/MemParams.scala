package fringe.templates.memory

import fringe.utils.DMap._
import fringe.utils.XMap._
import fringe.utils.log2Up


case class MemParams(
  iface: MemInterfaceType, // Required so the abstract MemPrimitive class can instantiate correct interface
  logicalDims: List[Int],
  bitWidth: Int,
  banks: List[Int],
  strides: List[Int],
  xBarWMux: XMap,
  xBarRMux: XMap, // muxPort -> accessPar
  directWMux: DMap,
  directRMux: DMap,  // muxPort -> List(banks, banks, ...)
  bankingMode: BankingMode,
  inits: Option[List[Double]] = None,
  syncMem: Boolean = false,
  fracBits: Int = 0,
  isBuf: Boolean = false,
  numActives: Int = 0,
  myName: String = "mem"
) {
  def depth: Int = logicalDims.product
  def hasXBarW: Boolean = xBarWMux.accessPars.sum > 0
  def hasXBarR: Boolean = xBarRMux.accessPars.sum > 0
  def numXBarW: Int = xBarWMux.accessPars.sum
  def widestXBarW: Int = if (xBarWMux.nonEmpty) xBarWMux.sortByMuxPortAndCombine.accessPars.max else 0
  def numXBarR: Int = xBarRMux.accessPars.sum
  def widestXBarR: Int = if (xBarRMux.nonEmpty) xBarRMux.sortByMuxPortAndCombine.accessPars.max else 0
  def numXBarWPorts: Int = xBarWMux.accessPars.size
  def numXBarRPorts: Int = xBarRMux.accessPars.size
  def hasDirectW: Boolean = directWMux.accessPars.sum > 0
  def hasDirectR: Boolean = directRMux.accessPars.sum > 0
  def numDirectW: Int = directWMux.accessPars.sum
  def numDirectR: Int = directRMux.accessPars.sum
  def numDirectWPorts: Int = directWMux.accessPars.size
  def numDirectRPorts: Int = directRMux.accessPars.size
  def xBarOutputs: Int = if (xBarRMux.nonEmpty) xBarRMux.sortByMuxPortAndCombine.accessPars.max else 0
  def directOutputs: Int = if (directRMux.nonEmpty) directRMux.sortByMuxPortAndCombine.accessPars.max else 0
  def totalOutputs: Int = xBarOutputs + directOutputs
  def numBanks: Int = banks.product
  def defaultDirect: List[List[Int]] = List(List.fill(banks.length)(99)) // Dummy bank address when ~hasDirectR
  def ofsWidth: Int = log2Up(depth/banks.product)
  def banksWidths: List[Int] = banks.map(log2Up)
  def elsWidth: Int = log2Up(depth) + 2
  def axes = xBarWMux.values.map(_._2).filter(_.isDefined)
  def axis: Int = if (axes.nonEmpty) axes.toList.head.get else -1 // Assume all shifters are in the same axis
}
