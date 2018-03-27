package emul

import java.text.SimpleDateFormat
import java.util.Calendar
import java.io.PrintStream

object Warn {
  val now = Calendar.getInstance().getTime
  val fmt = new SimpleDateFormat("dd_MM_yyyy_hh_mm_aa")
  val timestamp = fmt.format(now)
  var warns: Int = 0

  lazy val log = new PrintStream(timestamp + ".log")
  def apply(x: => String): Unit = {
    log.println(x)
    warns += 1
  }
  def close(): Unit = {
    if (warns > 0) {
      println(Warn.warns + " warnings occurred during program execution. See " + Warn.timestamp + ".log for details")
      log.close()
    }
  }
}