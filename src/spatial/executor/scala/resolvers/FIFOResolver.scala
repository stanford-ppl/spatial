package spatial.executor.scala.resolvers
import argon._
import spatial.executor.scala._
import spatial.node._
import spatial.executor.scala.memories._
import emul._
import spatial.metadata.memory._
import utils.implicits.collections.IterableHelpers

trait FIFOResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(FIFONew(depth)) =>
      new ScalaQueue[SomeEmul](execState.getValue[FixedPoint](depth).toInt)
    case Op(enq@FIFOEnq(fifo, data, ens)) =>
      type ET = fifo.A.L
      if (enq.isEnabled(execState)) {
        execState(fifo) match {
          case sq: ScalaQueue[EmulVal[ET]] =>
            sq.enq(execState(data) match { case ev: EmulVal[ET] => ev})
        }
      }
      EmulUnit(sym)

    case Op(deq@FIFODeq(fifo, ens)) if deq.isEnabled(execState) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => sq.deq()
      }

    case Op(isFull@FIFOIsFull(mem, _)) if isFull.isEnabled(execState) =>
      execState(mem) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(emul.Bool(sq.isFull))
      }


    case Op(isEmpty@FIFOIsEmpty(mem, _)) if isEmpty.isEnabled(execState) =>
      execState(mem) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(sq.isEmpty)
      }

    case Op(isAlmostFull@FIFOIsAlmostFull(fifo, _)) if isAlmostFull.isEnabled(execState) =>
      val wPar = fifo.writeWidths.maxOrElse(1)
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(Bool(sq.size + wPar >= sq.capacity))
      }

    case Op(isAlmostEmpty@FIFOIsAlmostEmpty(fifo, _)) if isAlmostEmpty.isEnabled(execState) =>
      val rPar = fifo.readWidths.maxOrElse(1)
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => SimpleEmulVal(Bool(sq.size <= rPar))
      }

    case Op(peek@FIFOPeek(fifo, _)) if peek.isEnabled(execState) =>
      execState(fifo) match {
        case sq: ScalaQueue[SomeEmul] => sq.head
      }

    case _ => super.run(sym, execState)
  }

}
