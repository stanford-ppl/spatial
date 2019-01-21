package fringe.templates.memory

import fringe.utils.DMap._
import fringe.utils.XMap._
import fringe.utils.NBufDMap._
import fringe.utils.NBufXMap._
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
  def createClone: MemParams = MemParams(iface,logicalDims,bitWidth,banks,strides,xBarWMux,xBarRMux,directWMux,directRMux,bankingMode,inits,syncMem,fracBits,isBuf,numActives,myName)
}


case class NBufParams(
  val mem: MemType,
  val logicalDims: List[Int],
  val numBufs: Int,
  val bitWidth: Int,
  val banks: List[Int],
  val strides: List[Int],
  val xBarWMux: NBufXMap,
  val xBarRMux: NBufXMap, // bufferPort -> (muxPort -> accessPar)
  val directWMux: NBufDMap,
  val directRMux: NBufDMap,  // bufferPort -> (muxPort -> List(banks, banks, ...))
  val broadcastWMux: XMap,
  val broadcastRMux: XMap,  // Assume broadcasts are XBar
  val bankingMode: BankingMode,
  val inits: Option[List[Double]] = None,
  val syncMem: Boolean = false,
  val fracBits: Int = 0,
  val numActives: Int = 1,
  val myName: String = "NBuf"
) {
  def hasXBarW = xBarWMux.accessPars.sum > 0
  def hasXBarR = xBarRMux.accessPars.sum > 0
  def numXBarW = xBarWMux.accessPars.sum
  def numXBarR = xBarRMux.accessPars.sum
  def numXBarWPorts = xBarWMux.accessPars.size
  def numXBarRPorts = xBarRMux.accessPars.size
  def hasDirectW = directWMux.mergeDMaps.accessPars.sum > 0
  def hasDirectR = directRMux.mergeDMaps.accessPars.sum > 0
  def numDirectW = directWMux.mergeDMaps.accessPars.sum
  def numDirectR = directRMux.mergeDMaps.accessPars.sum
  def numDirectWPorts = directWMux.accessPars.size
  def numDirectRPorts = directRMux.accessPars.size
  def hasBroadcastW = broadcastWMux.accessPars.toList.sum > 0
  def numBroadcastW = broadcastWMux.accessPars.toList.sum
  def numBroadcastWPorts = broadcastWMux.accessPars.toList.length
  def hasBroadcastR = broadcastRMux.accessPars.toList.sum > 0
  def numBroadcastR = broadcastRMux.accessPars.toList.sum
  def numBroadcastRPorts = broadcastRMux.accessPars.toList.length
  def totalOutputs = numXBarR + numDirectR + numBroadcastR
  def defaultDirect = List(List.fill(banks.length)(99))
  def portsWithWriter = (directWMux.keys ++ xBarWMux.keys).toList.sorted
  def depth = logicalDims.product + {if (mem == LineBufferType) (numBufs-1)*strides(0)*logicalDims(1) else 0} // Size of memory
  def N = logicalDims.length // Number of dimensions
  def ofsWidth = log2Up(depth/banks.product)
  def banksWidths = banks.map(log2Up(_))
  def flatXBarWMux = xBarWMux.mergeXMaps
  def flatXBarRMux = xBarRMux.mergeXMaps
  def flatDirectWMux = directWMux.mergeDMaps 
  def flatDirectRMux = directRMux.mergeDMaps 
  def combinedXBarWMux = flatXBarWMux.merge(broadcastWMux)
  def combinedXBarRMux = flatXBarRMux.merge(broadcastRMux)

}
