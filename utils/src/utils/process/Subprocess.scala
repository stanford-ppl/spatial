package utils.process

import java.io._

class Subprocess(args: String*)(react: (String,BufferedReader) => Option[String]) {
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
    if (dir.nonEmpty) pb.directory(new File(dir))
    p = pb.start()
    reader = new BufferedReader(new InputStreamReader(p.getInputStream))
    writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream))
    logger = new BufferedReader(new InputStreamReader(p.getErrorStream))
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
        // TODO[5]: What to do when process ended unexpectedly?
        isConnected = false // Process ended
      }
    }
    p.waitFor() // This is a java.lang.Process
  }

  def blockAndReturnOut(dir: String = ""): (Seq[String],Seq[String]) = {
    if (p eq null) run(dir)
    p.waitFor()
    var lines: Seq[String] = Nil
    var errs: Seq[String] = Nil
    var line = ""
    while (reader.ready && (line ne null)) {
      line = reader.readLine()
      if (line ne null) lines = line +: lines
    }
    line = ""
    while (logger.ready && (line ne null)) {
      line = logger.readLine()
      if (line ne null) errs = line +: errs
    }
    (lines.reverse, errs.reverse)
  }

  def kill(): Unit = if (p eq null) () else p.destroy()
}
