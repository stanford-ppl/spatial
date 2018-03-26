package spatial.lang

import argon._
import forge.tags._
import spatial.node._

import scala.collection.mutable.Queue

@ref class StreamIn[A:Bits] extends LocalMem0[A,StreamIn] with RemoteMem[A,StreamIn] with Ref[Queue[Any],StreamIn[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[StreamIn[A] <:< (LocalMem[A,StreamIn] with RemoteMem[A,StreamIn])]

  @api def value(): A = stage(StreamInRead(this,Set.empty))
  @api def value(en: Bit): A = stage(StreamInRead(this,Set(en)))
}
object StreamIn {
  @api def apply[A:Bits](bus: Bus): StreamIn[A] = stage(StreamInNew[A](bus))
}
