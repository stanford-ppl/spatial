package emul

import scala.collection.mutable

object DRAMTracker {
  val accessMap = mutable.Map[Any, Int]().withDefaultValue(0)

  override def toString: String = {
    "DRAM Tracker: " + accessMap.toString()
  }
}
