package spatial.executor.scala

import spatial.lang._

trait HWController {
  def reset(): Unit
  def init(curState: ExecutionState): Unit
  def tick(): Unit
  def isDone(): Boolean
}
