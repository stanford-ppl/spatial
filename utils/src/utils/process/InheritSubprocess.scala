package utils.process

import java.io._

class InheritSubprocess(args: String*) {
  private var p: Process = _

  def run(dir: String = ""): Unit = if (p eq null) {
    val pb = new ProcessBuilder(args:_*)
    if (dir.nonEmpty) pb.directory(new File(dir))
    pb.inheritIO()
    p = pb.start()
  } else {
    throw new Exception(s"Cannot run process $args while it is already running.")
  }

  def send(line: String): Unit = println(line)

  def block(dir: String = ""): Int = {
    if (p eq null) run(dir)
    p.waitFor() // This is a java.lang.Process
  }

  def kill(): Unit = if (p eq null) () else p.destroy()

}
