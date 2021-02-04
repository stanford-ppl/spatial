package spatial.lang.api

import argon._
import forge.tags._
import spatial.metadata.retiming._
import spatial.metadata.access._
import spatial.node._

trait PriorityDeqAPI {  this: Implicits =>
  @api def priorityDeq[T:Bits](fifo: FIFO[T]*): T = {
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id
    ForcedLatency(0.0) {
      val datas: Seq[T] = fifo.zipWithIndex.map { case (f, i) =>
        // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
        val d = stage(FIFOPriorityDeq(f, Set(fifo.take(i).map(_.isEmpty).fold(Bit(true)) { case (a: Bit, b: Bit) => a && b })))
        //      d.prDeqGrp = gid
        d.asInstanceOf[Sym[_]].prDeqGrp = fifo.head.toString.hashCode() // Base it on the first fifo
        d
      }
      val x = stage(PriorityMux[T](fifo.map(!_.isEmpty), datas.map { x => boxBits[T](x) }))
      x
    }
  }

  @api def priorityDeq[T:Bits](fifo: List[FIFO[T]], cond: List[Bit]): T = {
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id

    ForcedLatency(0.0) {
      assert(fifo.size == cond.size)

      // In a vacuum should a fifo have been enabled?
      val deqEnabled = (fifo zip cond).dropRight(1) map { case (a, b) => (!a.isEmpty) && b }

      // Have any prior fifos been enabled?
      val cumulativeEnabled = deqEnabled.scanLeft(Bit(false)) {
        _ || _
      }

      assert(cumulativeEnabled.size == fifo.size)

      val shouldDequeue = (cumulativeEnabled zip cond) map { case (a, b) => !a && b }
      assert(shouldDequeue.size == fifo.size)

      val datas: Seq[T] = (fifo zip shouldDequeue).map { case (f, c) =>
        // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
        val d = stage(FIFOPriorityDeq(f, Set(c)))
        d.asInstanceOf[Sym[_]].prDeqGrp = fifo.head.toString.hashCode() // Base it on the first fifo
        d
      }
      val x = stage(PriorityMux[T](fifo.zip(shouldDequeue).map { case (f, c) => !f.isEmpty && c }, datas.map { x => boxBits[T](x) }))
      x
    }
  }

  @api def roundRobinDeq[T:Bits](fifos: List[FIFO[T]], ens: List[Bit], iter: I32): T = {
    ForcedLatency(0.0) {
      assert(fifos.size == ens.size)

      // In a vacuum should a fifo have been enabled?
      val isEmpties = fifos map {
        _.isEmpty
      }
      val deqEnabled = (isEmpties zip ens) map { case (a, b) => (!a) && b }

      // Lower Priority is better
      // will rotate:
      // 0 1 2 3
      // 1 2 3 0
      // 2 3 0 1
      // 3 0 1 2
      val priorities = fifos.zipWithIndex.map {
        case (_, idx) =>
          (iter + I32(idx)) % I32(fifos.size)
      }

      val deqEnablesWithPriorities = deqEnabled zip priorities
      // dequeue if our condition is true, and nobody with a lower priority than us is enabled
      val shouldDequeue = (fifos zip ens).zipWithIndex map {
        case ((_, en), ind) =>
          val ourPriority = priorities(ind)
          val everyoneElse = (deqEnablesWithPriorities.take(ind) ++ deqEnablesWithPriorities.drop(ind + 1))
          val everyoneElseWeCareAbout = everyoneElse map {
            case (otherEn, otherInd) =>
              stage(Mux(otherInd < ourPriority, otherEn, Bit(false)))
          }
          val otherDequeue = everyoneElseWeCareAbout.reduce {
            _ || _
          }
          en && !otherDequeue
      }
      assert(shouldDequeue.size == fifos.size)


      val data: Seq[T] = (fifos zip shouldDequeue) map {
        case (f, c) =>
          val d = stage(FIFOPriorityDeq(f, Set(c)))
          d.asInstanceOf[Sym[_]].prDeqGrp = fifos.head.toString.hashCode
          d
      }

      stage(PriorityMux(
        fifos.zip(shouldDequeue) map { case (f, c) => c && !f.isEmpty },
        data.map { x => boxBits[T](x) }
      ))
    }
  }
}
