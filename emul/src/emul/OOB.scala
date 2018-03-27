package emul

import java.io.PrintStream
import java.io.File

object OOB {
  lazy val writeStream = new PrintStream("./logs/writes.log")
  def open(): Unit = {
    new File("./logs/").mkdirs()
    writeStream
  }
  def close(): Unit = {
    writeStream.close()
  }

  def readOrElse[T](mem: String, addr: String, invalid: T)(rd: => T): T = {
    try {
      rd
    }
    catch {case err: java.lang.ArrayIndexOutOfBoundsException =>
      invalid
    }
  }
  def writeOrElse(mem: String, addr: String, data: Any)(wr: => Unit): Unit = {
    try {
      wr
      writeStream.println(s"Mem: $mem; Addr: $addr; Data: $data")
    }
    catch {case err: java.lang.ArrayIndexOutOfBoundsException =>
      writeStream.println(s"Mem: $mem; Addr: $addr; Data: $data [OOB]")
    }
  }

}