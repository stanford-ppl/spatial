package utils

private class MemLog(var dir: String, interval: Long) extends Runnable {
  var isAlive: Boolean = true
  var start: Long = 0L
  def timestamp: Long = System.currentTimeMillis() - start
  private lazy val stream = io.stream.createStream(dir, "memory_usage.log")
  private lazy val runtime = Runtime.getRuntime
  private val mb = 1024 * 1024

  def note(str: String): Unit = stream.println(s"[$timestamp] $str")

  def run(): Unit = {
    while (isAlive) {
      val free = runtime.freeMemory()
      val total = runtime.totalMemory()
      val max = runtime.maxMemory()
      stream.println(s"[$timestamp] Used: ${(total - free)/mb}MB, Free: ${free/mb}MB, Total: ${total/mb}MB, Max: ${max/mb}MB")
      Thread.sleep(interval)
    }
    stream.close()
  }
}

class MemoryLogger private(watch: MemLog) {
  private lazy val thread = new Thread(watch)
  def start(dir: String = watch.dir): Unit = {
    watch.dir = dir
    watch.start = System.currentTimeMillis()
    thread.start()
  }
  def finish(): Unit = { watch.isAlive = false }
  def note(str: String): Unit = { watch.note(str) }

  def this(dir: String = ".", interval: Long = 200L) = this(new MemLog(dir,interval))
}
