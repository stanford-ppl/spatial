package spatial.executor.scala

sealed trait Status {
  def isFinished: Boolean = false
}
sealed trait Finished extends Status {
  override def isFinished: Boolean = true
}
case object Done extends Status with Finished
case object Indeterminate extends Status
case object Running extends Status
case object Disabled extends Status with Finished

trait OpExecutorBase {
  val execState: ExecutionState
  def tick(): Unit
  def status: Status
  def print(): Unit = {}
}

