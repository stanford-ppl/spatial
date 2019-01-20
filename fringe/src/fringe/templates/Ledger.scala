package fringe

import scala.collection.mutable._

/* Structure for keeping track of which ports on which interfaces are connected inside modules */

class ExposedPorts {
  var xBarR = ListBuffer[Int]()
  var directR = ListBuffer[Int]()
  var xBarW = ListBuffer[Int]()
  var directW = ListBuffer[Int]()
  var fixOpIn = ListBuffer[Int]()
  var fixFMAIn = ListBuffer[Int]()

  def addXBarR(p: Int): ExposedPorts = {xBarR = xBarR :+ p; this}
  def addDirectR(p: Int): ExposedPorts = {directR = directR :+ p; this}
  def addXBarW(p: Int): ExposedPorts = {xBarW = xBarW :+ p; this}
  def addDirectW(p: Int): ExposedPorts = {directW = directW :+ p; this}
  def addFixOpIn(p: Int): ExposedPorts = {fixOpIn = fixOpIn :+ p; this}
  def addFixFMAIn(p: Int): ExposedPorts = {fixFMAIn = fixFMAIn :+ p; this}
}

object Ledger {
  var connections = HashMap[Int, ExposedPorts]()

  def reset(port: Int): Unit = {connections -= port}
  def connectXBarR(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addXBarR(p))}
  def connectDirectR(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addDirectR(p))}
  def connectXBarW(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addXBarW(p))}
  def connectDirectW(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addDirectW(p))}
  def connectFixOpIn(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addFixOpIn(p))}
  def connectFixFMAIn(port: Int, p: Int) = {connections += (port -> connections.getOrElse(port, new ExposedPorts).addFixFMAIn(p))}
}
