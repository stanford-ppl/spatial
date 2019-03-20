package spatial.dse

import java.io._
import java.util.concurrent.{Executors, TimeUnit}

// TODO: Asynchronous error response
// TODO: Should give an iterator rather than the reader directly

class ExceptionWatcher(reader: BufferedReader) extends Runnable {
  //val log = new PrintStream("subproc.log")
  var isRunning = true

  def run(): Unit = while (isRunning) {
    val g = reader.readLine()
    //log.print(g)
    //log.flush()
    if (g ne null) println("[Subproc] " + g)
    /*if (lines.contains("Traceback")) {
      Thread.sleep(1000)
      while (reader.ready()) {
        println(reader.readLine())
      }
      throw new Exception("Child process failed")
    }*/
  }
}

case class Subproc(args: String*)(react: (String,BufferedReader) => Option[String]) {
  private var reader: BufferedReader = _
  private var writer: BufferedWriter = _
  private var logger: BufferedReader = _
  private var p: Process = _
  //private val pool = Executors.newFixedThreadPool(1)
  //private var watcher: ExceptionWatcher = _

  private def println(x: String): Unit = {
    writer.write(x)
    writer.newLine()
    writer.flush()
  }

  def run(dir: Option[String] = None): Unit = if (p eq null) {
    val pb = new ProcessBuilder(args:_*)
    dir.foreach{d => pb.directory(new File(d)) }
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)

    p = pb.start()
    reader = new BufferedReader(new InputStreamReader(p.getInputStream))
    writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream))
    logger = new BufferedReader(new InputStreamReader(p.getErrorStream))
    //watcher = new ExceptionWatcher(logger)
    //pool.submit(watcher)
  } else {
    throw new Exception(s"Cannot run process $args while it is already running.")
  }

  def block(dir: Option[String] = None): Int = {
    if (p eq null) run(dir)
    var isConnected = true
    while (isConnected) {
      // Otherwise react to the stdout of the subprocess
      val input = reader.readLine()
      if (input ne null) {
        val response = react(input,reader)
        response.foreach{r => println(r) }
      }
      else {
        isConnected = false // Process ended (TODO: unexpectedly?)
      }
    }
    //watcher.isRunning = false
    //pool.shutdownNow()
    p.waitFor(30, TimeUnit.SECONDS)
    p.exitValue()
  }

}
