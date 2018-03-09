package utils.io

import java.io.{ByteArrayOutputStream, OutputStream, PrintStream}

class CaptureOutputStream extends OutputStream {
  val data = new ByteArrayOutputStream()

  override def write(b: Int): Unit = data.write(b)
  override def write(b: Array[Byte]): Unit = data.write(b)
  override def write(b: Array[Byte], off: Int, len: Int): Unit = data.write(b,off,len)

  def dump: String = new java.lang.String(data.toByteArray, java.nio.charset.StandardCharsets.UTF_8)
}

class CaptureStream(__out: CaptureOutputStream, paired: PrintStream) extends PrintStream(__out) {
  def this(paired: PrintStream) = this(new CaptureOutputStream(), paired)
  def dump: String = __out.dump
  //TODO[5]: For some reason this duplicates the printing
  //override def print(s: String): Unit = { paired.print(s); super.print(s) }
  //override def println(s: String): Unit = { paired.println(s); super.println(s) }
}