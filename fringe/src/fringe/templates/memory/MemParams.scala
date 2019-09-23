package fringe.templates.memory

import fringe.Ledger._
import fringe.utils.Access
import utils.math._

case class MemParams(
                      iface: MemInterfaceType, // Required so the abstract MemPrimitive class can instantiate correct interface
                      Ds: List[Int],
                      bitWidth: Int,
                      Ns: List[Int],
                      Bs: List[Int],
                      Ps: List[Int],
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
  def volume: Int = utils.math.volume(Ns, Bs, Ps, Ds)
  def widestR: Int = RMapping.map(_.par).sorted.reverse.headOption.getOrElse(0)
  def widestW: Int = WMapping.map(_.par).sorted.reverse.headOption.getOrElse(0)
  def totalOutputs: Int = RMapping.map(_.par).sum
  def axes: Seq[Int] = WMapping.collect{case w if (w.shiftAxis.isDefined) => w.shiftAxis.get}
  def axis: Int = if (axes.nonEmpty) axes.toList.head else -1 // Assume all shifters are in the same axis
  def lookupW(accHash: OpHash): Access = WMapping.collect{case x if x.accHash == accHash => x}.head
  def lookupR(accHash: OpHash): Access = RMapping.collect{case x if x.accHash == accHash => x}.head
  def lookupWBase(accHash: OpHash): Int = WMapping.indexWhere(_.accHash == accHash)
  def lookupRBase(accHash: OpHash): Int = RMapping.indexWhere(_.accHash == accHash)
  def elsWidth: Int = utils.math.elsWidth(volume)
  def numBanks: Int = utils.math.numBanks(Ns)
  def ofsWidth: Int = utils.math.ofsWidth(volume, Ns)
  def banksWidths: Seq[Int] = utils.math.banksWidths(Ns)
  def createClone: MemParams = MemParams(iface,Ds,bitWidth,Ns,Bs,Ps,WMapping,RMapping,bankingMode,inits,syncMem,fracBits,isBuf,numActives,myName)
}


case class NBufParams(
  val numBufs: Int,
  val mem: MemType,
  val p: MemParams
) {
  def depth = p.volume + {if (mem == LineBufferType) (numBufs-1)*p.Ps(0)*p.Ds(1) else 0} // Size of memory
  def totalOutputs = p.totalOutputs
  def ofsWidth = log2Up(p.volume/p.Ns.product)
  def banksWidths = p.Ns.map(log2Up(_))
  def portsWithWriter: List[Int] = p.WMapping.collect{case x if x.port.bufPort.isDefined => x.port.bufPort.get}.distinct
}
