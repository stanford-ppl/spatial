package spatial.executor.scala.resolvers
import argon._
import spatial.executor.scala._
import spatial.node._
import spatial.executor.scala.memories._
import emul._
import spatial.metadata.memory._
import utils.implicits.collections.IterableHelpers

trait FIFOResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = op match {
    case FIFONew(depth) =>
      new ScalaQueue[SomeEmul](execState.getValue[FixedPoint](depth).toInt)
    case enq@FIFOEnq(fifo, data, ens) =>
      type ET = fifo.A.L
      if (enq.isEnabled(execState)) {
        execState(fifo) match {
          case sq: ScalaQueue[EmulVal[ET]] =>
            implicit def state: argon.State = execState.IR
            dbgs(s"Enqueuing to $fifo")
            sq.enq(execState(data) match { case ev: EmulVal[ET] => ev})
            dbgs(s"$fifo now has ${sq.size} elements")
        }
      }
      EmulUnit(sym)

    case deq@FIFODeq(fifo, ens) if deq.isEnabled(execState) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] =>
          implicit def state: argon.State = execState.IR
          dbgs(s"Dequeuing from $fifo [size = ${sq.size - 1}]")
          sq.deq()
      }

    case isFull@FIFOIsFull(mem, _) if isFull.isEnabled(execState) =>
      execState(mem) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(emul.Bool(sq.isFull))
      }


    case isEmpty@FIFOIsEmpty(mem, _) if isEmpty.isEnabled(execState) =>
      execState(mem) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(sq.isEmpty)
      }

    case isAlmostFull@FIFOIsAlmostFull(fifo, _) if isAlmostFull.isEnabled(execState) =>
      val wPar = fifo.writeWidths.maxOrElse(1)
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(Bool(sq.size + wPar >= sq.capacity))
      }

    case isAlmostEmpty@FIFOIsAlmostEmpty(fifo, _) if isAlmostEmpty.isEnabled(execState) =>
      val rPar = fifo.readWidths.maxOrElse(1)
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(Bool(sq.size <= rPar))
      }

    case peek@FIFOPeek(fifo, _) if peek.isEnabled(execState) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => sq.head
      }

    case numel@FIFONumel(fifo, _) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(emul.FixedPoint.fromInt(sq.size))
      }

    case fvd@FIFOVecDeq(fifo, adr, ens) if fvd.isEnabled(execState) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] =>
          EmulVector(adr.map{ _ => sq.deq()})
      }

    case _ => super.run(sym, op, execState)
  }

}
