package fringe.utils

import scala.collection.immutable.ListMap
import fringe.Ledger._
import emul.ResidualGenerator.ResidualGenerator

object AccessHelper {
  def singular(bitWidth: Int): Access = Access(0, 0, 0, List(0), List(0), None, PortInfo(None, 1, 1, List(1), bitWidth, List(List(ResidualGenerator(1,0,0)))))
  def singularOnPort(port: Int, bitWidth: Int): Access = Access(port, 0, 0, List(0), List(0), None, PortInfo(Some(port), 1, 1, List(1), bitWidth, List(List(ResidualGenerator(1,0,0)))))
}

case class PortInfo(
  val bufPort: Option[Int],
  val portWidth: Int,
  val ofsWidth: Int,
  val banksWidth: List[Int],
  val dataWidth: Int,
  val visibleBanks: List[List[ResidualGenerator]]
){
  def randomBanks: PortInfo = PortInfo(bufPort, portWidth, ofsWidth, banksWidth, dataWidth, List.tabulate(portWidth){i => List.tabulate(banksWidth.size){j => ResidualGenerator(1,0,0)}})
}
case class Access(
  val accHash: OpHash,
  val muxPort: Int,
  val muxOfs: Int,
  val castgroup: List[Int],
  val broadcast: List[Int],
  val shiftAxis: Option[Int],
  val port: PortInfo
) {
  def par: Int = castgroup.size
  def randomBanks: Access = Access(accHash, muxPort, muxOfs, castgroup, broadcast, shiftAxis, port.randomBanks)
}
