package spatial.executor.scala

import argon.Sym
import spatial.node._
import spatial.lang._

import spatial.metadata.control._

class InnerForeach(original: OpForeach) extends HWController with LoopedController {
  override def reset(): Unit = {}

  // Current iteration values of each iter symbol
  override var iterStates: Seq[(I32, IterState)] = Seq.empty

  override def init(curState: ExecutionState): Unit = {

  }

  override def tick(): Unit = {

  }

  override def isDone(): Boolean = isLastIter
}
