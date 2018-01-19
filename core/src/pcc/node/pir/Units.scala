package pcc.node
package pir

import forge._
import pcc.core._
import pcc.data.Effects
import pcc.lang._

sealed abstract class ConfigBlackBox extends Control
@op case class GEMM[T](a: T, b: T, y: T) extends ConfigBlackBox
@op case class GEMV[T](a: T, b: T, y: T) extends ConfigBlackBox
@op case class CONV[T](a: T, b: T, y: T) extends ConfigBlackBox

abstract class PU extends Control
object PU {
  @api def compute(datapath: Block[Void], iters: Seq[Seq[I32]]): Void = {
    stage(PCU(datapath, iters))
  }
  @api def memory(memories: Seq[Sym[_]]): PMU = PMU(memories,None,Nil,None,Nil)
}

@op case class PCU(
  datapath: Block[Void],
  iters:    Seq[Seq[I32]],
) extends PU {
  override def inputs: Seq[Sym[_]] = syms(datapath)
  override def binds: Seq[Sym[_]] = super.binds ++ iters.flatten
}

@op case class PMU(
  memories:  Seq[Sym[_]],
  var readAddr:  Option[Block[Void]] = None,
  var readIters: Seq[Seq[I32]] = Nil,
  var writeAddr: Option[Block[Void]] = None,
  var writeIters: Seq[Seq[I32]] = Nil
) extends PU {
  override def inputs: Seq[Sym[_]] = syms(readAddr) ++ syms(writeAddr)
  override def binds:  Seq[Sym[_]] = super.binds ++ writeIters.flatten ++ readIters.flatten
}

abstract class GlobalBus[T:Sym] extends Alloc[T] {
  override def effects: Effects = Effects.Mutable
}
@op case class VectorBus() extends GlobalBus[Lanes]
@op case class ScalarBus() extends GlobalBus[Word]
@op case class ControlBus() extends GlobalBus[Bit]
