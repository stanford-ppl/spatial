package pcc.spade.node

import pcc.core._
import pcc.lang.{Box, Vec, Bit}
import pcc.lang.pir.{In, Out}

sealed trait Direction
case object N  extends Direction
case object NE extends Direction
case object E  extends Direction
case object SE extends Direction
case object S  extends Direction
case object SW extends Direction
case object W  extends Direction
case object NW extends Direction

abstract class Module[B:Box] extends Op[B] {
  //def wires: Seq[Sym[_]] = inputs
  //def names: Seq[String]

  //override def elems: Seq[(String,Sym[_])] = names.zip(wires)

  // Set x and y co-ordinates using Scala getter setter
  private var _x = 0
  def x = _x
  def x_= (value: Int): Unit = _x = value

  private var _y = 0
  def y = _y
  def y_= (value: Int): Unit = _y = value

}

abstract class PUSpec {
  def cIns    : List[Direction] // Control input directions
  def cOuts   : List[Direction] // Control output directions
  def sIns    : List[Direction] // Scalar input directions
  def sOuts   : List[Direction] // Scalar output directions
  def vIns    : List[Direction] // Vector input directions
  def vOuts   : List[Direction] // Vector output directions

  def nVIns  = vIns.size
  def nVOuts = vOuts.size
  def nSIns  = sIns.size
  def nSOuts = sOuts.size
  def nCIns  = cIns.size
  def nCOuts = cOuts.size
}

abstract class PUModule[B:Box] extends Module[B] {
  def cIns:  Seq[In[Bit]]
  def cOuts: Seq[Out[Bit]]
  def sIns:  Seq[In[Vec[Bit]]]
  def sOuts: Seq[Out[Vec[Bit]]]
  def vIns:  Seq[In[Vec[Vec[Bit]]]]
  def vOuts: Seq[Out[Vec[Vec[Bit]]]]
  def spec:  PUSpec

  override def binds  = super.binds ++ cIns ++ cOuts ++ sIns ++ sOuts ++ vIns ++ vOuts

  def vIn(d: Direction): List[In[Vec[Vec[Bit]]]] = {
    spec.vIns.zipWithIndex.filter { _._1 == d }.map { _._2 }.map { vIns(_) }
  }

  def vIn(d: Direction, idx: Int): Option[In[Vec[Vec[Bit]]]] = {
    val ins = spec.vIns.zipWithIndex.filter { _._1 == d }.map { _._2 }
    if (ins.size >= (idx+1)) Some(vIns(ins(idx))) else None
  }

  def vOut(d: Direction): List[Out[Vec[Vec[Bit]]]] = {
    spec.vOuts.zipWithIndex.filter { _._1 == d }.map { _._2 }.map { vOuts(_) }
  }

  def vOut(d: Direction, idx: Int): Option[Out[Vec[Vec[Bit]]]] = {
    val outs = spec.vOuts.zipWithIndex.filter { _._1 == d }.map { _._2 }
    if (outs.size >= (idx+1)) Some(vOuts(outs(idx))) else None
  }

  def vIO(d: Direction) = vIn(d) ++ vOut(d)
}
//case class In[B:Box,A:Bits](box: B, name: String) extends Op[A]
