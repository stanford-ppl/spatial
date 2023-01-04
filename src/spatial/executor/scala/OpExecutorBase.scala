package spatial.executor.scala

trait OpExecutorBase {
  val execState: ExecutionState
  def tick(): Unit
  def isDone: Boolean
}

