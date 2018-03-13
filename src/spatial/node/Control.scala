package spatial.node

import forge.tags._
import core._
import spatial.lang._

@op case class CounterNew[A:Num](start: Num[A], end: Num[A], step: Num[A], par: I32) extends Alloc[Counter[A]] {
  val nA: Num[A] = Num[A]
}
@op case class ForeverNew() extends Alloc[Counter[I32]] {
  override def effects: Effects = Effects.Unique
}

@op case class CounterChainNew(counters: Seq[Counter[_]]) extends Alloc[CounterChain]

@op case class AccelScope(block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = mirror(f)
}

@op case class UnitPipe(block: Block[Void], ens: Set[Bit]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class OpForeach(
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32],
  ens:    Set[Bit]
) extends Loop[Void] {
  def cchains = Seq(cchain -> iters)
  def bodies = Seq(iters -> Seq(block))
}


@op case class OpReduce[A](
  cchain: CounterChain,
  accum:  Reg[A],
  map:    Block[A],
  load:   Lambda1[Reg[A],A],
  reduce: Lambda2[A,A,A],
  store:  Lambda2[Reg[A],A,Void],
  ident:  Option[Bits[A]],
  fold:   Option[Bits[A]],
  iters:  List[I32],
  ens:    Set[Bit],
)(implicit val A: Bits[A]) extends Loop[Void] {
  override def cchains = Seq(cchain -> iters)
  override def bodies  = Seq(iters -> Seq(map,load,reduce,store))
}

@op case class MemReduceBlackBox[A,C[T]](
  cchainMap: CounterChain,
  accum:     C[A],
  map:       Block[C[A]],
  reduce:    Lambda2[A,A,A],
  ident:     Option[Bits[A]],
  fold:      Boolean,
  itersMap:  Seq[I32]
)(implicit val A: Bits[A], C: LocalMem[A,C]) extends EarlyBlackBox


@op case class OpMemReduce[A,C[T]](
  cchainMap: CounterChain,
  cchainRed: CounterChain,
  accum:     C[A],
  map:       Block[C[A]],
  loadRes:   Lambda1[C[A],A],
  loadAcc:   Lambda1[C[A],A],
  reduce:    Lambda2[A,A,A],
  storeAcc:  Lambda2[C[A],A,Void],
  ident:     Option[Bits[A]],
  fold:      Boolean,
  itersMap:  Seq[I32],
  itersRed:  Seq[I32],
  ens:       Set[Bit],
)(implicit val A: Bits[A], C: LocalMem[A,C]) extends Loop[Void] {
  override def iters: Seq[I32] = itersMap ++ itersRed
  override def cchains = Seq(cchainMap -> itersMap, cchainRed -> itersRed)
  override def bodies = Seq(
    itersMap -> Seq(map),
    (itersMap ++ itersRed) -> Seq(loadRes,reduce),
    itersRed -> Seq(loadAcc, storeAcc)
  )
}

@op case class StateMachine[A](
  start:     Bits[A],
  notDone:   Lambda1[A,Bit],
  action:    Lambda1[A,Void],
  nextState: Lambda1[A,A],
  ens:       Set[Bit],
)(implicit val A: Bits[A]) extends Loop[Void] {
  override def iters: Seq[I32] = Nil
  override def cchains = Nil
  override def bodies = Seq(Nil -> Seq(notDone, action, nextState))
}


@op case class UnrolledForeach(
  cchain:  CounterChain,
  func:    Block[Void],
  iterss:  Seq[Seq[I32]],
  validss: Seq[Seq[Bit]],
  ens:     Set[Bit],
) extends UnrolledLoop[Void] {
  override def cchainss = Seq(cchain -> iterss)
  override def bodiess = Seq(iterss -> Seq(func))
}

@op case class UnrolledReduce(
  cchain:  CounterChain,
  func:    Block[Void],
  iterss:  Seq[Seq[I32]],
  validss: Seq[Seq[Bit]],
  ens:     Set[Bit],
) extends UnrolledLoop[Void] {
  override def cchainss = Seq(cchain -> iterss)
  override def bodiess = Seq(iterss -> Seq(func))
}