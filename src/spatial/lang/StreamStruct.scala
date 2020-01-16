package spatial.lang

import argon._
import forge.tags._
import spatial.node._

/**
  * StreamStruct is a struct of decoupled datatypes, such that reading a field is a dequeue operation on that particular stream
  *
  * @tparam A
  */
trait StreamStruct[A] extends Top[A] with Ref[Nothing,A] {
  override val __neverMutable = false
  val box: A <:< StreamStruct[A]
  private implicit lazy val A: StreamStruct[A] = this.selfType

  @rig def field[F:Bits](name: String): F = StreamStruct.field[A,F](me, name)

  @rig private def __field[F](name: String, tp: Bits[_]): Sym[F] = {
    implicit val F: Bits[F] = tp.asInstanceOf[Bits[F]]
    F.boxed(StreamStruct.field[A,F](me, name))
  }

  def fields: Seq[(String,ExpType[_,_])]
  @rig def fieldMap: Seq[(String,Exp[_,_])] = fields.map{case (name,tp) => (name, __field(name, tp.asInstanceOf[Bits[_]])) }

  @api override def neql(that: A): Bit = fieldMap.zip(box(that).fieldMap).map{case ((_, a: Bits[_]), (_,b: Bits[_])) => a !== b}.reduce{_|_}
  @api override def eql(that: A): Bit = fieldMap.zip(box(that).fieldMap).map{case ((_, a: Bits[_]), (_,b: Bits[_])) => a === b}.reduce{_&_}
}

object StreamStruct {
  def tp[S:StreamStruct]: StreamStruct[S] = implicitly[StreamStruct[S]]

  @rig def apply[S:StreamStruct](elems: (String,Sym[_])*): S = stage(SimpleStreamStruct[S](elems))
  @rig def field[S:StreamStruct,A:Bits](struct: S, name: String): A = stage(FieldDeq[S,A](StreamStruct.tp[S].box(struct),name,Set[Bit]()))
  @rig def field_update[S:StreamStruct,A:Type](struct: S, name: String, data: A): Void = stage(FieldEnq[S,A](StreamStruct.tp[S].box(struct),name,data))
}