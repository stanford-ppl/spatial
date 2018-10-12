package emul

import scala.collection.mutable

object StatTracker {
  private val stats = mutable.Map[Any, Int]().withDefaultValue(0)
  private val enable = mutable.Stack[Boolean](false)

  def change(stat: Any, change: Int): Unit = {
    if (enable.top) {
      stats(stat) += change
    }
  }

  def pushState(state: Boolean): Unit = {
    enable.push(state)
  }

  def popState(): Boolean = {
    enable.pop()
  }

  override def toString: String = {
    "StatTracker: " + stats.toString()
  }
}
