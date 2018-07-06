package emul

import java.io.PrintStream
import java.io.File

object OOB {
  lazy val writeStream = new PrintStream("./logs/writes.log")
  lazy val readStream = new PrintStream("./logs/reads.log")
  def open(): Unit = {
    new File("./logs/").mkdirs()
    writeStream
    readStream
  }
  def close(): Unit = {
    writeStream.close()
    readStream.close()
  }

  def readOrElse[T](mem: String, addr: String, invalid: T, en: Boolean)(rd: => T): T = {
    try {
      val data = rd
      if (en) readStream.println(s"Mem: $mem; Addr: $addr")
      data
    }
    catch {case err: java.lang.ArrayIndexOutOfBoundsException =>
      if (en) readStream.println(s"Mem: $mem; Addr: $addr [OOB]")
      invalid
    }
  }
  def writeOrElse(mem: String, addr: String, data: Any, en: Boolean)(wr: => Unit): Unit = {
    try {
      wr
      if (en) writeStream.println(s"Mem: $mem; Addr: $addr; Data: $data")
    }
    catch {case err: java.lang.ArrayIndexOutOfBoundsException =>
      if (en) writeStream.println(s"Mem: $mem; Addr: $addr; Data: $data [OOB]")
    }
  }

}