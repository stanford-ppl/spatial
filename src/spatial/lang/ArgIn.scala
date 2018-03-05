package spatial.lang

import core._
import forge.tags._
import forge.Ptr

import spatial.node._

@ref class ArgIn[A:Bits] extends LocalMem[A,ArgIn] with RemoteMem[A,ArgIn] with Ref[Ptr[Any],ArgIn[A]] {
  val tA: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[ArgIn[A] <:< (LocalMem[A,ArgIn] with RemoteMem[A,ArgIn])]

  @api def value: A = stage(ArgInRead(this))
}
object ArgIn {
  @api def apply[A:Bits]: ArgIn[A] = stage(ArgInNew(Bits[A].zero))
}
