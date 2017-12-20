package pcc.util

import java.io._

case class Subproc(args: String*)(react: (String,BufferedReader) => Option[String]) {
  private var reader: BufferedReader = _
  private var writer: BufferedWriter = _
  private var logger: BufferedReader = _
  private var p: Process = _

  private def println(x: String): Unit = {
    writer.write(x)
    writer.newLine()
    writer.flush()
  }

  def run(dir: String = ""): Unit = if (p eq null) {
    val pb = new ProcessBuilder(args:_*)
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)
    if (dir.nonEmpty) pb.directory(new File(dir))
    p = pb.start()
    reader = new BufferedReader(new InputStreamReader(p.getInputStream))
    writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream))
    logger = new BufferedReader(new InputStreamReader(p.getErrorStream))
    //watcher = new ExceptionWatcher(logger)
    //pool.submit(watcher)
  } else {
    throw new Exception(s"Cannot run process $args while it is already running.")
  }

  def send(line: String): Unit = println(line)

  def block(dir: String = ""): Int = {
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
    p.waitFor() // This is a java.lang.Process
  }

  def blockAndReturnOut(dir: String = ""): Seq[String] = {
    if (p eq null) run(dir)
    p.waitFor()
    var lines: Seq[String] = Nil
    var line = ""
    while (reader.ready && (line ne null)) {
      line = reader.readLine()
      if (line ne null) lines = line +: lines
    }
    lines.reverse
  }

  def kill(): Unit = if (p eq null) throw new Exception("Process has not started") else p.destroy()
}
