package argon

import forge.tags._
import argon.transform.Transformer

import scala.collection.mutable

sealed abstract class SetBy
object SetBy {
  /** Metadata is set by the user. */
  case object User extends SetBy { override def toString: String = "SymData.User" }
  /** Metadata is set by a @flow rule. */
  case object Flow {
    /** Metadata is set by a @flow rule on itself. */
    case object Self extends SetBy { override def toString: String = "SymData.Flow.Self" }
    /** Metadata is set by a @flow rule of one of its consumers. */
    case object Consumer extends SetBy { override def toString: String = "SymData.Flow.Consumer" }
  }
  /** Metadata is set by an analysis pass. */
  case object Analysis {
    /** Metadata is set by visiting that node in an analysis pass. */
    case object Self extends SetBy { override def toString: String = "SymData.Analysis.Self" }
    /** Metadata is set by visiting some consumer in an analysis pass. */
    case object Consumer extends SetBy { override def toString: String = "SymData.Analysis.Consumer" }
  }
}
object GlobalData {
  /** Metadata is set by the user. */
  case object User extends SetBy { override def toString: String = "GlobalData.User" }
  /** Metadata is set by a @flow. */
  case object Flow extends SetBy { override def toString: String = "GlobalData.Flow" }
  /** Metadata is set by an analysis pass. */
  case object Analysis extends SetBy { override def toString: String = "GlobalData.Analysis" }
}

object Transfer extends Enumeration {
  type Transfer = Value
  val Remove, Ignore, Mirror = Value

  def apply(src: SetBy): Transfer = src match {
    case SetBy.User          => Mirror
    case SetBy.Flow.Self     => Ignore
    case SetBy.Flow.Consumer => Remove
    case SetBy.Analysis.Self => Mirror
    case SetBy.Analysis.Consumer => Remove
    case GlobalData.User       => Ignore
    case GlobalData.Flow       => Remove
    case GlobalData.Analysis   => Remove
  }
}

/** Any kind of IR graph metadata.
  * Transfer determines how metadata is transferred across Transformers.
  *   Ignore - Nothing is explicitly done. Metadata is assumed to be updated by a @flow or else dropped
  *   Mirror - Metadata is mirrored (using its mirror rule) and explicitly transferred
  *   Remove - Metadata is dropped (explicitly removed) during symbol transformation
  *
  * For consistency, global analysis data is dropped before transformers are run if it is Mirror or Remove.
  *
  * If you're not sure which one is right, use the SetBy subclasses instead to specify how the
  * metadata is created.
  */
abstract class Data[T](val transfer: Transfer.Transfer) { self =>
  final type Tx = Transformer

  type Transfer = Transfer.Transfer

  def this(setBy: SetBy) = this(Transfer(setBy))

  /** Defines how to copy metadata during mirroring/updating. */
  def mirror(f: Tx): T = this.asInstanceOf[T]

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()
}



/** Shortcuts for metadata */
object metadata {
  type MMap = mutable.Map[Class[_],Data[_]]

  private def keyOf[M<:Data[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  def add(edge: Sym[_], key: Class[_], m: Data[_]): Unit = edge._data += (key -> m)

  def add[M<:Data[M]:Manifest](edge: Sym[_], m: M): Unit = edge._data += (m.key -> m)
  def add[M<:Data[M]:Manifest](edge: Sym[_], m: Option[M]): Unit = m match {
    case Some(data) => edge._data += (data.key -> data)
    case None => edge._data.remove(keyOf[M])
  }
  def all(edge: Sym[_]): Iterator[(Class[_],Data[_])] = edge._data.iterator

  def remove(edge: Sym[_], key: Class[_]): Unit = edge._data.remove(key)

  def clear[M<:Data[M]:Manifest](edge: Sym[_]): Unit = edge._data.remove(keyOf[M])

  def apply[M<:Data[M]:Manifest](edge: Sym[_]): Option[M] = edge._data.get(keyOf[M]).map(_.asInstanceOf[M])
}

@data object globals {
  def add[M<:Data[M]:Manifest](m: M): Unit = state.globals.add[M](m)
  def apply[M<:Data[M]:Manifest]: Option[M] = state.globals[M]
  def clear[M<:Data[M]:Manifest]: Unit = state.globals.clear[M]
  def invalidateBeforeTransform(): Unit = state.globals.invalidateBeforeTransform()

  def foreach(func: (Class[_],Data[_]) => Unit): Unit = state.globals.foreach(func)
}
