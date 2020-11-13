package spatial.lang.api

import argon._
import forge.tags._
import spatial.lang.api.PriorityDeqAPI.getNewGroupId
import spatial.metadata.retiming._
import spatial.node._

object PriorityDeqAPI {
  private var rng = scala.util.Random
  def getNewGroupId: Int = rng.nextInt
}

trait PriorityDeqAPI {  this: Implicits =>
  @api def priorityDeq[T:Bits](fifo: FIFO[T]*): T = {
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id
    val datas: Seq[T] = fifo.zipWithIndex.map{case (f,i) =>
      // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
      val d = stage(FIFOPriorityDeq(f, Set(fifo.take(i).map(_.isEmpty).fold(Bit(true)){case (a: Bit,b: Bit) => a && b})))
      d
    }
    val x = stage(PriorityMux[T](fifo.map(!_.isEmpty), datas.map{x => boxBits[T](x)}))
    x
  }
  @api def priorityDeq[T:Bits](fifo: List[FIFO[T]], cond: List[Bit]): T = {
    assert(fifo.size == cond.size, s"Fifo list has size ${fifo.size}, but cond has length ${cond.size}")
    val gid = ctx.line // TODO: this is probably an unsafe way to compute a group id
    val datas: Seq[T] = fifo.zipWithIndex.map{case (f,i) =>
      // The enable should also have a !f.isEmpty but this messes up II analysis so it is added at codegen as a hack for this deq node
      val d = stage(FIFOPriorityDeq(f, Set(fifo.take(i).map(_.isEmpty).fold(Bit(true)){case (a: Bit,b: Bit) => a && b}, cond(i))))
      d
    }

    // And _.isEmpty with cond(i)
    val priority_enables = (fifo zip cond) map {case (fifo, c) => !fifo.isEmpty && c}
    val x = stage(PriorityMux[T](priority_enables, datas.map{x => boxBits[T](x)}))
    x
  }
}
