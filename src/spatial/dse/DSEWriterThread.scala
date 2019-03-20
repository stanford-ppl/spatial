package spatial.dse

import java.io.PrintStream
import java.util.concurrent.BlockingQueue

case class DSEWriterThread(
  threadId:  Int,
  spaceSize: BigInt,
  filename:  String,
  header:    String,
  workQueue: BlockingQueue[Array[String]]
) extends Runnable {

  private var isAlive: Boolean = true
  private var hasTerminated: Boolean = false
  def requestStop(): Unit = { isAlive = false }

  def run(): Unit = {
    val data = new PrintStream(filename)
    data.println(header + ",Timestamp")

    val P = BigDecimal(spaceSize)
    var N = BigDecimal(0)
    var nextNotify = BigDecimal(0); val notifyStep = 5000
    val startTime = System.currentTimeMillis()

    while(isAlive) {
      try {
        val array = workQueue.take()
        if (array.nonEmpty) {
          array.foreach { line => data.println(line) }
          data.flush()

          N += array.length
          if (N > nextNotify) {
            val time = System.currentTimeMillis - startTime
            println("  %.4f".format(100 * (N / P).toFloat) + s"% ($N / $P) Complete after ${time / 1000} seconds")
            nextNotify += notifyStep
          }
        }
        else if (array.isEmpty) requestStop() // Somebody poisoned the work queue!
      }
      catch {case e: Throwable =>
        println(e.getMessage)
        e.getStackTrace.foreach{line => println("  " + line) }
        requestStop()
      }
    }

    data.close()
    hasTerminated = true
  }
}
