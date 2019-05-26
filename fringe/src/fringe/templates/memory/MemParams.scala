package fringe.templates.memory

import fringe.Ledger._
import fringe.utils.log2Up
import emul.ResidualGenerator._
import fringe.utils.{PortInfo, Access}

case class MemParams(
  iface: MemInterfaceType, // Required so the abstract MemPrimitive class can instantiate correct interface
  logicalDims: List[Int],
  darkVolume: Int,
  bitWidth: Int,
  banks: List[Int],
  strides: List[Int],
  WMapping: List[Access],
  RMapping: List[Access],
  bankingMode: BankingMode,
  inits: Option[List[Double]] = None,
  syncMem: Boolean = false,
  fracBits: Int = 0,
  isBuf: Boolean = false,
  numActives: Int = 1,
  myName: String = "mem"
) {
  def depth: Int = logicalDims.product + darkVolume
  def widestR: Int = RMapping.map(_.par).sorted.reverse.headOption.getOrElse(0)
  def widestW: Int = WMapping.map(_.par).sorted.reverse.headOption.getOrElse(0)
  def totalOutputs: Int = RMapping.map(_.par).sum
  def numBanks: Int = banks.product
  def bankDepth: Int = 
    if (banks.size == logicalDims.size) banks.zip(logicalDims).map{case (b, d) => math.ceil(d.toDouble / b.toDouble).toInt}.product + scala.math.ceil(darkVolume.toDouble / banks.product.toDouble).toInt
    else math.ceil(depth.toDouble / banks.product.toDouble).toInt
  def ofsWidth: Int = log2Up(bankDepth)
  def banksWidths: List[Int] = banks.map(log2Up)
  def elsWidth: Int = log2Up(depth) + 2
  def axes: Seq[Int] = WMapping.collect{case w if (w.shiftAxis.isDefined) => w.shiftAxis.get}
  def axis: Int = if (axes.nonEmpty) axes.toList.head else -1 // Assume all shifters are in the same axis
  def lookupW(accHash: OpHash): Access = WMapping.collect{case x if x.accHash == accHash => x}.head
  def lookupR(accHash: OpHash): Access = RMapping.collect{case x if x.accHash == accHash => x}.head
  def lookupWBase(accHash: OpHash): Int = WMapping.indexWhere(_.accHash == accHash)
  def lookupRBase(accHash: OpHash): Int = RMapping.indexWhere(_.accHash == accHash)
  def createClone: MemParams = MemParams(iface,logicalDims,darkVolume,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits,isBuf,numActives,myName)
}


case class NBufParams(
  val numBufs: Int,
  val mem: MemType,
  val p: MemParams
) {
  def depth = p.logicalDims.product + {if (mem == LineBufferType) (numBufs-1)*p.strides(0)*p.logicalDims(1) else 0} + p.darkVolume// Size of memory
  def totalOutputs = p.totalOutputs
  def ofsWidth = log2Up(p.bankDepth)
  def banksWidths = p.banks.map(log2Up(_))
  def portsWithWriter: List[Int] = p.WMapping.collect{case x if x.port.bufPort.isDefined => x.port.bufPort.get}.distinct
}
