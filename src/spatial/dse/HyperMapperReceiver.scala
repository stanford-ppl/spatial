package spatial.dse

import argon._
import java.io.BufferedReader
import java.util.concurrent.BlockingQueue

import scala.io.Source
import spatial.metadata.params._
import spatial.metadata.bounds._

import scala.util.Try
import scala.util.matching.Regex


case class HyperMapperReceiver(
  input:      BufferedReader,
  workOut:    BlockingQueue[Seq[DesignPoint]],
  requestOut: BlockingQueue[DSERequest],
  doneOut:    BlockingQueue[Boolean],
  space:      Seq[Domain[_]],
  HEADER:     String,
  THREADS:    Int,
  DIR:        String
)(implicit state: State) extends Runnable {

  private var running: Boolean = true

  def processPoints(cmd: String, nPoints: Int, in: BufferedReader, file: Option[String]): Unit = {
    val head    = in.readLine()
    val header  = head.split(",").map(_.trim)
    val order   = space.map{d => header.indexOf(d.name) }
    if (order.exists(_ < 0)) {
      bug(s"[Master] Received Line: $head")
      order.zipWithIndex.filter{case (idx, i) => idx < 0 }.foreach{case (idx, i) =>
        bug(s"Header was missing: ${space(i).name}")
      }
      throw new Exception("Incorrect header in request")
    }

    try {
      println(s"[Master] Received Line: $head")
      val blockSize = if (nPoints < 500) 5 else 500
      requestOut.put( DSERequest(nPoints, file) )

      (0 until nPoints by blockSize).foreach{i =>
        val block = Seq.tabulate(Math.min(blockSize, nPoints - i)){_ =>
          val point = in.readLine()
          val values = point.split(",").map(_.trim.toLowerCase).map {
            case "true" => true
            case "false" => false
            case x => x.toInt
          }
          Point(order.map{ i => values(i) }) : DesignPoint
        }
        //println(s"Created block of ${block.length}")
        workOut.put(block)
      }
    }
    catch {case t: Throwable =>
      bug(s"$cmd")
      bug(s"$head")
      bug(s"${t.getMessage}")
      throw t
    }
  }

  object Request {
    lazy val regex: Regex = raw"Request ([0-9]*)".r
    def unapply(x: String): Option[Int] = x match {
      case regex(n) => Try(n.toInt).toOption
      case _ => None
    }
  }
  object FRequest {
    lazy val regex: Regex = raw"FRequest ([0-9]*) (.*)".r
    def unapply(x: String): Option[(Int,String)] = x match {
      case regex(n, file) => Try(n.toInt).toOption.map{n => (n,file) }
      case _ => None
    }
  }

  def run(): Unit = {
    try {
      while (running) {
        // TODO: May want to add a timeout on the read
        val cmd = input.readLine()
        if (cmd eq null) running = false
        else {
          println(s"[Master] Received Line: $cmd")
          cmd match {
            case Request(n) =>
              processPoints(cmd, n, input, None)

            case FRequest(n,file) =>
              val absFile = if (file.startsWith("/")) file else s"$DIR/$file"
              println(s"Reading requests from file: $absFile")
              val f = Source.fromFile(absFile)
              processPoints(cmd, n, f.bufferedReader(), Some(absFile))
              f.close()

            case "End"            => running = false
            case "Pareto"         => running = false
          }
        }
      }

      // Poison the work and request queues to end other processes
      println("Ending work queue.")
      (0 until THREADS).foreach{_ => workOut.put(Seq.empty) }
      requestOut.put(DSERequest(-1,None))
      doneOut.put(true)
    }
    catch {case t: Throwable =>
      println("Terminating unexpectedly...")
      t.printStackTrace()
      (0 until THREADS).foreach{_ => workOut.put(Seq.empty) }
      requestOut.put(DSERequest(-1,None))
      doneOut.put(false)
    }
  }

}
