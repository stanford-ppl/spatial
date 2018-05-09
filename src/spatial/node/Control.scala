package spatial.node

import forge.tags._
import argon._
import spatial.lang._
import spatial.util._

@op case class CounterNew[A:Num](start: Num[A], end: Num[A], step: Num[A], par: I32) extends Alloc[Counter[A]] {
  val A: Num[A] = Num[A]
  override def effects: Effects = Effects.Unique
}
@op case class ForeverNew() extends Alloc[Counter[I32]] {
  override def effects: Effects = Effects.Unique
}

@op case class CounterChainNew(counters: Seq[Counter[_]]) extends Alloc[CounterChain] {
  override def effects: Effects = Effects.Unique
}

@op case class AccelScope(block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def mayBeOuterBlock(i: Int) = true
  override def cchains = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = mirror(f)

  // TODO[5]: Technically Accel doesn't need a simple effect - prevents removal of Accel in ALL cases
  override def effects: Effects = super.effects andAlso Effects.Simple
}

@op case class UnitPipe(ens: Set[Bit], block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
  override def mayBeOuterBlock(i: Int) = true
}

@op case class ParallelPipe(ens: Set[Bit], block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
  override def mayBeOuterBlock(i: Int): Boolean = true
}

@op case class OpForeach(
  ens:    Set[Bit],
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32]
) extends Loop[Void] {
  def cchains = Seq(cchain -> iters)
  def bodies = Seq(iters -> Seq(block))
  override def mayBeOuterBlock(i: Int) = true

  // A Foreach with 1 iteration is really a UnitPipe
  @rig override def rewrite: Void = {
    if (cchain.isUnit) stage(UnitPipe(ens, block))
    else super.rewrite
  }
}


@op case class OpReduce[A](
  ens:    Set[Bit],
  cchain: CounterChain,
  accum:  Reg[A],
  map:    Block[A],
  load:   Lambda1[Reg[A],A],
  reduce: Lambda2[A,A,A],
  store:  Lambda2[Reg[A],A,Void],
  ident:  Option[A],
  fold:   Option[A],
  iters:  List[I32]
)(implicit val A: Bits[A]) extends Loop[Void] {
  override def binds: Set[Sym[_]] = super.binds ++ reduce.inputs
  override def cchains = Seq(cchain -> iters)
  override def bodies  = Seq(iters -> Seq(map,reduce), Nil -> Seq(load,store))
  override def mayBeOuterBlock(i: Int) = i == 0
}

@op case class OpMemReduce[A,C[T]](
  ens:       Set[Bit],
  cchainMap: CounterChain,
  cchainRed: CounterChain,
  accum:     C[A],
  map:       Block[C[A]],
  loadRes:   Lambda1[C[A],A],
  loadAcc:   Lambda1[C[A],A],
  reduce:    Lambda2[A,A,A],
  storeAcc:  Lambda2[C[A],A,Void],
  ident:     Option[A],
  fold:      Boolean,
  itersMap:  Seq[I32],
  itersRed:  Seq[I32]
)(implicit val A: Bits[A], val C: LocalMem[A,C]) extends Loop[Void] {
  override def binds: Set[Sym[_]] = super.binds ++ reduce.inputs
  override def iters: Seq[I32] = itersMap ++ itersRed
  override def cchains = Seq(cchainMap -> itersMap, cchainRed -> itersRed)
  override def bodies = Seq(
    itersMap -> Seq(map),
    (itersMap ++ itersRed) -> Seq(loadRes, reduce),
    itersRed -> Seq(loadAcc, storeAcc)
  )
  override def mayBeOuterBlock(i: Int): Boolean = i == 0
}

@op case class StateMachine[A](
  ens:       Set[Bit],
  start:     Bits[A],
  notDone:   Lambda1[A,Bit],
  action:    Lambda1[A,Void],
  nextState: Lambda1[A,A]
)(implicit val A: Bits[A]) extends Loop[Void] {
  override def binds = super.binds ++ notDone.inputs
  override def iters: Seq[I32] = Nil
  override def cchains = Nil
  override def bodies = Seq(Nil -> Seq(notDone, action, nextState))
  override def mayBeOuterBlock(i: Int): Boolean = i == 1
}


@op case class UnrolledForeach(
  ens:     Set[Bit],
  cchain:  CounterChain,
  func:    Block[Void],
  iterss:  Seq[Seq[I32]],
  validss: Seq[Seq[Bit]]
) extends UnrolledLoop[Void] {
  override def cchainss = Seq(cchain -> iterss)
  override def bodiess = Seq(iterss -> Seq(func))
  override def mayBeOuterBlock(i: Int): Boolean = true
}

@op case class UnrolledReduce(
  ens:     Set[Bit],
  cchain:  CounterChain,
  func:    Block[Void],
  iterss:  Seq[Seq[I32]],
  validss: Seq[Seq[Bit]]
) extends UnrolledLoop[Void] {
  override def cchainss = Seq(cchain -> iterss)
  override def bodiess = Seq(iterss -> Seq(func))
  override def mayBeOuterBlock(i: Int): Boolean = true
}