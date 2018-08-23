package utils

import java.io.PrintStream

import scala.collection.mutable

class OutlierFinder(val finderName: String) {
  OutlierFinder.set += this

  private val times = new mutable.HashMap[String, mutable.Map[Any,(Long,Long)]]()

  def record[R](name: String, key: Any)(scope: => R): R = {
    if (!times.contains(name)) times += name -> new mutable.HashMap[Any,(Long,Long)]()
    val startTime = System.currentTimeMillis()
    try {
      scope
    }
    finally {
      val endTime = System.currentTimeMillis()
      val (n,prev) = times(name).getOrElse(key, (0L, 0L))
      times(name) = times(name) + (key -> (n+1, prev + endTime - startTime))
    }
  }

  def report(stream: PrintStream = Console.out): Unit = {
    times.foreach{case (cat,map) =>
      val avgs = map.toSeq.map{case (name,(n,len)) => (name, len.toDouble/n) }
      val values = avgs.map(_._2)
      val mean = stats.mean(values)
      val stdDev = stats.stdDev(values)
      val outliers = avgs.filter{case (_,avg) => Math.abs(avg - mean) > 2*stdDev }
      if (outliers.nonEmpty) {
        stream.println(s"$cat")
        stream.println("------")
        stream.println(s"Mean:   $mean")
        stream.println(s"StdDev: $stdDev")
        stream.println(s"Outliers: ")
        outliers.foreach{case (name,time) => stream.println(s"  $name: $time") }
        stream.println("")
        stream.println("")
      }
    }

    times.foreach{case (cat,map) =>
      val avgs = map.toSeq.map{case (name,(n,len)) => (name, len.toDouble/n) }
      val values = avgs.map(_._2)
      val mean = stats.mean(values)
      val stdDev = stats.stdDev(values)
      stream.println(s"$cat")
      stream.println("------")
      stream.println(s"Mean:   $mean")
      stream.println(s"StdDev: $stdDev")
      avgs.sortBy(_._2).foreach{case (name,time) => stream.println(s"  $name: $time") }
      stream.println("")
      stream.println("")
    }
  }

}

object OutlierFinder {
  var set: Set[OutlierFinder] = Set.empty

  def report(stream: PrintStream = Console.out): Unit = set.foreach(_.report(stream))
}