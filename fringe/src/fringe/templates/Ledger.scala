package fringe

import scala.collection.mutable._

/* Structure for keeping track of which ports on which interfaces are connected inside modules */
case class RAddr(val port: Int, val lane: Int)
class ExposedPorts {
  var xBarR = ListBuffer[RAddr]()
  var directR = ListBuffer[RAddr]()
  var xBarW = ListBuffer[Int]()
  var directW = ListBuffer[Int]()
  var broadcastW = ListBuffer[Int]()
  var broadcastR = ListBuffer[RAddr]()
  var output = ListBuffer[Int]()

  def addXBarR(p: RAddr): ExposedPorts = {xBarR = xBarR :+ p; this}
  def addDirectR(p: RAddr): ExposedPorts = {directR = directR :+ p; this}
  def addXBarW(p: Int): ExposedPorts = {xBarW = xBarW :+ p; this}
  def addDirectW(p: Int): ExposedPorts = {directW = directW :+ p; this}
  def addBroadcastW(p: Int): ExposedPorts = {broadcastW = broadcastW :+ p; this}
  def addBroadcastR(p: RAddr): ExposedPorts = {broadcastR = broadcastR :+ p; this}
  def addOutput(p: Int): ExposedPorts = {output = output :+ p; this}
}

object Ledger {
  var connections = HashMap[Int, ExposedPorts]()

  def reset(hash: Int): Unit = {connections -= hash}
  def connectXBarR(hash: Int, p: Int, lane: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addXBarR(RAddr(p,lane)))}
  def connectDirectR(hash: Int, p: Int, lane: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addDirectR(RAddr(p,lane)))}
  def connectXBarW(hash: Int, p: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addXBarW(p))}
  def connectDirectW(hash: Int, p: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addDirectW(p))}
  def connectBroadcastW(hash: Int, p: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addBroadcastW(p))}
  def connectBroadcastR(hash: Int, p: Int, lane: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addBroadcastR(RAddr(p,lane)))}
  def connectOutput(hash: Int, p: Int) = {connections += (hash -> connections.getOrElse(hash, new ExposedPorts).addOutput(p))}

}
