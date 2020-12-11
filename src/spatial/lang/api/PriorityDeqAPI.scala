package spatial.lang.api

import argon._
import forge.tags._
import spatial.metadata.retiming._
import spatial.metadata.access._
import spatial.node._

trait PriorityDeqAPI {  this: Implicits =>
  @api def priorityDeq[T:Bits](fifo: FIFO[T]*): T = {
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id
    val datas: Seq[T] = fifo.zipWithIndex.map{case (f,i) =>
      // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
      val d = stage(FIFOPriorityDeq(f, Set(fifo.take(i).map(_.isEmpty).fold(Bit(true)){case (a: Bit,b: Bit) => a && b})))
//      d.prDeqGrp = gid
      d.asInstanceOf[Sym[_]].prDeqGrp = fifo.head.toString.hashCode() // Base it on the first fifo
      d
    }
    val x = stage(PriorityMux[T](fifo.map(!_.isEmpty), datas.map{x => boxBits[T](x)}))
    x
  }
//  @api def priorityDeq[T:Bits](fifo: List[FIFO[T]], cond: List[Bit]): T = {
//    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id
//    val datas: Seq[T] = fifo.zipWithIndex.map{case (f,i) =>
//      // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
//      val d = stage(FIFOPriorityDeq(f, Set(fifo.take(i).map(_.isEmpty).fold(Bit(true)){case (a: Bit,b: Bit) => a && b}, cond(i))))
//      d.asInstanceOf[Sym[_]].prDeqGrp = fifo.head.toString.hashCode() // Base it on the first fifo
//      d
//    }
//    val x = stage(PriorityMux[T](fifo.zip(cond).map{case (f,c) => !f.isEmpty && c}, datas.map{x => boxBits[T](x)}))
//    x
//  }

  @api def priorityDeq[T:Bits](fifo: List[FIFO[T]], cond: List[Bit]): T = {
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id

    assert(fifo.size == cond.size)
    // dequeue from fifo if prior fifos are either empty or have empty conditions.

    val shouldDequeue = scala.collection.mutable.ListBuffer[Bit]()
    // For the first fifo we only check the condition, since isEmpty is checked via codegen hack
    shouldDequeue.append(cond(0))
    // for subsequent fifos we check if the previous one is enabled and not empty, and if not, check our cond.
    (fifo.dropRight(1) zip cond.drop(1)) foreach {
      case (prevFifo, cnd) =>
        val previousEnabled = shouldDequeue.last && (!prevFifo.isEmpty)
        val currentEnabled = !previousEnabled && cnd
        shouldDequeue.append(currentEnabled)
    }

    assert(shouldDequeue.size == fifo.size)

    val datas: Seq[T] = (fifo zip shouldDequeue).map{case (f,c) =>
      // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
      val d = stage(FIFOPriorityDeq(f, Set(c)))
      d.asInstanceOf[Sym[_]].prDeqGrp = fifo.head.toString.hashCode() // Base it on the first fifo
      d
    }
    val x = stage(PriorityMux[T](fifo.zip(cond).map{case (f,c) => !f.isEmpty && c}, datas.map{x => boxBits[T](x)}))
    x
  }
}
