package pcc.data.static

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.node._
import pcc.util.DAG


trait HelpersControl {
  def getCChains(block: Block[_]): Seq[CounterChain] = getCChains(block.stms)
  def getCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}

  def ctrlIters(ctrl: Ctrl): Seq[I32] = ctrl match {
    case Ctrl(Op(loop: Loop), -1) => loop.iters
    case Ctrl(Op(loop: Loop), i)  => loop.bodies(i)._1
    case _ => Nil
  }

  def ctrDef(x: Counter): CounterNew = x match {
    case Op(c: CounterNew) => c
    case _ => throw new Exception(s"Could not find counter definition for $x")
  }
  def cchainDef(x: CounterChain): CounterChainNew = x match {
    case Op(c: CounterChainNew) => c
    case _ => throw new Exception(s"Could not find counterchain definition for $x")
  }

  implicit class CounterChainHelperOps(x: CounterChain) {
    def ctrs: Seq[Counter] = cchainDef(x).counters
  }

  implicit class CounterHelperOps(x: Counter) {
    def start: I32 = ctrDef(x).start
    def step: I32 = ctrDef(x).step
    def end: I32 = ctrDef(x).end
    def ctrPar: I32 = ctrDef(x).par
  }

  implicit class IndexHelperOps(i: I32) {
    def ctrStart: I32 = ctrOf(i).start
    def ctrStep: I32 = ctrOf(i).step
    def ctrEnd: I32 = ctrOf(i).end
    def ctrPar: I32 = ctrOf(i).ctrPar
    def ctrParOr1: Int = ctrOf.get(i).map(_.ctrPar.toInt).getOrElse(1)
  }


  /**
    * Returns the least common ancestor (LCA) of the two controllers.
    * If the controllers have no ancestors in common, returns None.
    */
  def LCA(a: Ctrl, b: Ctrl): Option[Ctrl] = DAG.LCA(a, b){ctrlParent}
  def LCA(a: Option[Ctrl], b: Option[Ctrl]): Option[Ctrl] = (a,b) match {
    case (Some(c1),Some(c2)) => LCA(c1,c2)
    case _ => None
  }

  /**
    * Returns the controller parent of this controller
    */
  @stateful def ctrlParent(ctrl: Ctrl): Option[Ctrl] = ctrl.id match {
    case -1 => parentOf.get(ctrl.sym)
    case _  => Some(Ctrl(ctrl.sym, -1))
  }

  /**
    * Returns the symbols of all ancestor controllers of this symbol
    * Ancestors are ordered outermost to innermost
    */
  @stateful def symParents(sym: Sym[_], stop: Option[Sym[_]] = None): Seq[Sym[_]] = {
    DAG.ancestors(sym, stop){p => parentOf.get(p).map(_.sym) }
  }

  /**
    * Returns all ancestor controllers from this controller (inclusive) to optional stop (inclusive)
    * Ancestors are ordered outermost to innermost
    */
  @stateful def ctrlParents(ctrl: Ctrl): Seq[Ctrl] = DAG.ancestors(ctrl,None){ctrlParent}
  @stateful def ctrlParents(ctrl: Ctrl, stop: Option[Ctrl]): Seq[Ctrl] = DAG.ancestors(ctrl,stop){ctrlParent}
  @stateful def ctrlParents(sym: Sym[_], stop: Option[Ctrl] = None): Seq[Ctrl] = {
    parentOf.get(sym).map(p => ctrlParents(p,stop)).getOrElse(Nil)
  }

  /**
    * Returns a list of controllers from the start (inclusive) to the end (exclusive).
    * Controllers are ordered outermost to innermost.
    */
  @stateful def ctrlBetween(start: Option[Ctrl], end: Option[Ctrl]): Seq[Ctrl] = {
    if (start.isEmpty) Nil
    else {
      val path = ctrlParents(start.get, end)
      if (path.headOption == end) path.drop(1) else path
    }
  }

  @stateful def ctrlBetween(access: Sym[_], mem: Sym[_]): Seq[Ctrl] = {
    ctrlBetween(parentOf.get(access), parentOf.get(mem))
  }

}
