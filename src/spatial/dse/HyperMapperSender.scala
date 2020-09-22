package spatial.dse

import java.io.{BufferedWriter, FileWriter}
import java.util.concurrent.BlockingQueue

case class HyperMapperSender(
  output:    BufferedWriter,
  requestIn: BlockingQueue[DSERequest],
  resultIn:  BlockingQueue[Array[String]],
  doneOut:   BlockingQueue[Boolean],
  HEADER:    String
) extends Runnable {

  private var running: Boolean = true
  private var earlyExit: Boolean = false

  private def write(x: String, out: BufferedWriter): Boolean = {
    try {
      out.write(x)
      out.newLine()
      true
    }
    catch {case e: java.io.IOException =>
      false
    }
  }

  def run(): Unit = {
    while (running) {
      val DSERequest(nPoints, file) = requestIn.take()
      if (nPoints >= 0) {
        val out = file.map{str => new BufferedWriter(new FileWriter(str+".out")) }.getOrElse(output)
        println(s"[Sender] Sending back: $HEADER")
        running &= write(HEADER, out)
        val P = nPoints
        var N = 0
        var next: Int = Math.min(5000, nPoints)
        val start = System.currentTimeMillis()
        while (N < nPoints && running) {
          val points = resultIn.take()
          //println(s"[Sender] Got block of ${points.length}")
          points.foreach{point =>
            running &= write(point, out)
          }
          out.flush()
          N += points.length
          if (N >= next) {
            val time = System.currentTimeMillis() - start
            println(s"$N / $P complete after ${time/1000} seconds: " + "%.2f".format(time.toDouble / N) + " ms/pt")
            next += 5000
          }
        }
        if (!running) earlyExit = true
        if (file.isDefined) {
          out.close()
          System.out.println("File complete: Sending ACK back to HyperMapper")
          write(s"Ready ${file.get}.out", output)
          output.flush()
        }
      }
      else {
        running = false
      }
    }

    if (earlyExit) doneOut.put(false)
  }

}
