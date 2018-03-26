package spatial.lang

import argon._
import forge.tags._

import spatial.node._

import scala.collection.mutable.Queue

@ref class StreamOut[A:Bits] extends LocalMem0[A,StreamOut] with RemoteMem[A,StreamOut] with Ref[Queue[Any],StreamOut[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[StreamOut[A] <:< (LocalMem[A,StreamOut] with RemoteMem[A,StreamOut])]

  @api def :=(data: A): Void = stage(StreamOutWrite(this,data,Set.empty))
  @api def :=(data: A, en: Bit): Void = stage(StreamOutWrite(this,data,Set(en)))
}
object StreamOut {
  @api def apply[A:Bits](bus: Bus): StreamOut[A] = stage(StreamOutNew[A](bus))
}
