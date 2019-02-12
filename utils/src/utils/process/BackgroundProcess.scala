package utils.process

import java.io._
import java.util.concurrent.TimeUnit

case class BackgroundProcess(dir: String, args: List[String]) {
  private var reader: BufferedReader = _
  private var writer: BufferedWriter = _
  private var logger: BufferedReader = _
  private var p: Process = _

  def send(line: String): Unit = {
    if (p eq null) run()
    try {
      writer.write(line + "\n")
      writer.flush()
    }
    catch {case _:Throwable =>
      checkErrors()
    }
  }
  
  def checkErrors(): Unit = if (logger.ready) {
    var errs: Seq[String] = Nil
    var line = ""
    while (logger.ready && (line ne null)) {
      line = logger.readLine()
      if (line ne null) errs = line +: errs
    }
    if (errs.nonEmpty) throw new Exception(s"Subprocess $args returned error(s):\n${errs.mkString("\n")}")
  }

  def run(): Unit = if (p eq null) {
    import scala.collection.JavaConverters._
    val pb = new ProcessBuilder(args.asJava)
    if (dir.nonEmpty) pb.directory(new File(dir))
    //pb.redirectError(Redirect.INHERIT)
    p = pb.start()
    reader = new BufferedReader(new InputStreamReader(p.getInputStream))
    writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream))
    logger = new BufferedReader(new InputStreamReader(p.getErrorStream))
  } else {
    throw new Exception(s"Cannot run process $args while it is already running.")
  }

  def blockOnChar(): Char = {
    if (p eq null) run()
    val c = reader.read()
    checkErrors()
    c.toChar
  }

  def blockOnLine(): String = {
    if (p eq null) run()
    val response = reader.readLine()
    checkErrors()
    response
  }

  def kill(wait: Long = 0): Unit = {
    if (!(p eq null)) p.destroy()
    if (wait > 0) p.waitFor(wait, TimeUnit.MILLISECONDS)
  }

  def waitFor(): Int = {
    writer.close()
    p.getOutputStream.close()
    p.waitFor()
  }
}

object BackgroundProcess {
  def apply(dir: String, varargs: String*) = new BackgroundProcess(dir, varargs.toList)
}
