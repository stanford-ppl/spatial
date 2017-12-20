package pcc.util

import java.io.OutputStream

class NullOutputStream() extends OutputStream {
  override def write(b: Int) { }
  override def write(b: Array[Byte]) { }
  override def write(b: Array[Byte], off: Int, len: Int) { }
}
