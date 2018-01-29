package pcc.node
package pir

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.lang.pir._

import scala.collection.mutable

sealed abstract class ConfigBlackBox[T:Bits] extends Primitive[T]
@op case class GEMMConfig[T:Bits](a: T, b: T) extends ConfigBlackBox[T]
@op case class GEMVConfig[T:Bits](a: T, b: T) extends ConfigBlackBox[T]
@op case class CONVConfig[T:Bits](a: T, b: T) extends ConfigBlackBox[T]
@op case class StreamConfig1[T:Bits](a1: T) extends ConfigBlackBox[T]
@op case class StreamConfig2[T:Bits](a1: T, a2: T) extends ConfigBlackBox[T]
@op case class StreamConfig3[T:Bits](a1: T, a2: T, a3: T) extends ConfigBlackBox[T]
@op case class StreamConfig4[T:Bits](a1: T, a2: T, a3: T, a4: T) extends ConfigBlackBox[T]

@op case class CounterChainCopy(ctrs: Seq[Counter]) extends Alloc[CounterChain]

abstract class PU extends Control {
  def ins: mutable.Map[Int,In[_]]
  def outs: mutable.Map[Int,Out[_]]
  override def binds = super.binds ++ ins.values ++ outs.values
}

// Virtual switch (outer controller)
@op case class VSwitch(
  scope: Block[Void],
  iters: Seq[I32],
  ins:   mutable.Map[Int,In[_]] = mutable.Map.empty,
  outs:  mutable.Map[Int,Out[_]] = mutable.Map.empty
) extends PU {
  override def cchains = scope.stms.collect{case s: CounterChain => s}
  override def inputs = syms(scope)
  override def binds = super.binds ++ iters
}

// Address Generator
@op case class AG(
  datapath: Block[Void],
  iterss:   Seq[Seq[I32]],
  ins:   mutable.Map[Int,In[_]] = mutable.Map.empty,
  outs:  mutable.Map[Int,Out[_]] = mutable.Map.empty
) extends PU {
  override def cchains = getCChains(datapath)
  override def iters   = iterss.flatten
  override def inputs  = syms(datapath)
  override def binds   = super.binds ++ iterss.flatten
}

// Virtual compute unit
@op case class VPCU(
  datapath: Block[Void],
  iterss:   Seq[Seq[I32]],
  ins:   mutable.Map[Int,In[_]] = mutable.Map.empty,
  outs:  mutable.Map[Int,Out[_]] = mutable.Map.empty
) extends PU {
  override def cchains = getCChains(datapath)
  override def iters   = iterss.flatten
  override def inputs  = syms(datapath)
  override def binds   = super.binds ++ iterss.flatten
}

// Virtual memory unit
@op case class VPMU(
  memories:    Seq[Sym[_]],
  var rdPath:  Option[Block[_]] = None,
  var rdIters: Seq[Seq[I32]] = Nil,
  var wrPath:  Option[Block[_]] = None,
  var wrIters: Seq[Seq[I32]] = Nil,
  ins:   mutable.Map[Int,In[_]] = mutable.Map.empty,
  outs:  mutable.Map[Int,Out[_]] = mutable.Map.empty
) extends PU {
  override def cchains = rdPath.map(getCChains).getOrElse(Nil) ++ wrPath.map(getCChains).getOrElse(Nil)
  override def iters = rdIters.flatten ++ wrIters.flatten
  override def inputs = syms(rdPath) ++ syms(wrPath) ++ syms(memories)
  override def binds  = super.binds ++ iters

  def getWdata(): Option[Sym[_]] = {
    wrPath.flatMap { b =>
      b.stms.find { case s @ Op(_:Data) => true; case _ => false }
    }
  }

  def setWr(addr: Block[_], iters: Seq[Seq[I32]]): Unit = {
    this.wrPath = Some(addr); this.wrIters = iters
  }
  def setRd(addr: Block[_], iters: Seq[Seq[I32]]): Unit = {
    this.rdPath = Some(addr); this.rdIters = iters
  }
}


