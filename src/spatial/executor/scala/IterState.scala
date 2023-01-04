package spatial.executor.scala

case class IterState(current: Int, start: Int, stop: Int, step: Int) {
  def isLast: Boolean = {
    if (step > 0) {
      current + step >= stop
    } else {
      current + step <= stop
    }
  }
}