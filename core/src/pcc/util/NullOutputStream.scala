package pcc.util

import java.io.OutputStream

class NullOutputStream() extends OutputStream {
  override def write(b: Int): Unit = ()
  override def write(b: Array[Byte]): Unit = ()
  override def write(b: Array[Byte], off: Int, len: Int): Unit = ()
}
