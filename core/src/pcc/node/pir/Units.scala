package pcc.node.pir

import forge._
import pcc.core._
import pcc.data.Effects
import pcc.lang._
import pcc.node.{Alloc, Control}

sealed abstract class ConfigBlackBox extends Control
@op case class GEMM[T](a: T, b: T, y: T) extends ConfigBlackBox
@op case class GEMV[T](a: T, b: T, y: T) extends ConfigBlackBox
@op case class CONV[T](a: T, b: T, y: T) extends ConfigBlackBox

abstract class PU extends Control

@op case class PCU(
  cchains:  Seq[CounterChain],
  datapath: Block[Void],
  iters:    Seq[Seq[I32]],
) extends PU {
  override def inputs: Seq[Sym[_]] = syms(cchains) ++ syms(datapath)
  override def binds: Seq[Sym[_]] = super.binds ++ iters.flatten
}

@op case class PMU(
  memories: Seq[SRAM[_]],
  cchains:  Seq[CounterChain],
  datapath: Block[Void],
  iters:    Seq[Seq[I32]]
) extends PU {
  override def inputs: Seq[Sym[_]] = syms(cchains) ++ syms(datapath)
  override def binds:  Seq[Sym[_]] = super.binds ++ iters.flatten
}

abstract class GlobalBus[T:Sym] extends Alloc[T] {
  override def effects: Effects = Effects.Mutable
}
@op case class VectorBus() extends GlobalBus[Lanes]
@op case class ScalarBus() extends GlobalBus[Word]
@op case class ControlBus() extends GlobalBus[Bit]
