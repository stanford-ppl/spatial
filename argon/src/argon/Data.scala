package argon

import forge.tags._
import argon.transform.TransformerInterface

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

/** Transfer determines how metadata is transferred across Transformers.
  * Symbol metadata:
  *   Mirror - Metadata is mirrored (using its mirror rule) and explicitly transferred
  *   Remove - Metadata is dropped (explicitly removed) during symbol transformation
  *   Ignore - Nothing is explicitly done (metadata is dropped if not set by some external rule)
  *
  * Global metadata:
  *   Mirror - Nothing is explicitly done (metadata is assumed to be stable or  updated explicitly)
  *   Remove - Metadata is dropped (explicitly removed) prior to transformation
  *   Ignore - Nothing is explicitly done (metadata is assumed to be stable or  updated explicitly)
  */
object Transfer extends Enumeration {
  type Transfer = Value
  val Remove, Mirror, Ignore = Value

  def apply(src: SetBy): Transfer = src match {
    case SetBy.User              => Mirror
    case SetBy.Flow.Self         => Mirror
    case SetBy.Flow.Consumer     => Remove
    case SetBy.Analysis.Self     => Mirror
    case SetBy.Analysis.Consumer => Remove
    case GlobalData.User         => Mirror
    case GlobalData.Flow         => Remove
    case GlobalData.Analysis     => Remove
  }
}

/** Any kind of IR graph metadata.

  *
  * For consistency, global analysis data is dropped before transformers are run if it is Mirror or Remove.
  *
  * If you're not sure which one is right, use the SetBy subclasses instead to specify how the
  * metadata is created.
  */
abstract class Data[T](val transfer: Transfer.Transfer) extends Serializable { self =>
  final type Tx = TransformerInterface

  type Transfer = Transfer.Transfer

  def this(setBy: SetBy) = this(Transfer(setBy))

  /** Defines how to copy metadata during mirroring/updating. */
  def mirror(f: Tx): T = this.asInstanceOf[T]

  def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()
}



/** Shortcuts for metadata */
object metadata {
  var dbgPrint: Boolean = false
  val dbgPrintClasses: Set[String] = Set()

  type MMap = mutable.Map[Class[_],Data[_]]

  private def keyOf[M<:Data[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  def add(edge: Sym[_], key: Class[_], m: Data[_]): Unit = {
    if (dbgPrint && (dbgPrintClasses contains key.getSimpleName)) {
      println(s"Adding: $edge[$key] = $m")
    }
    edge._data += (key -> m)
  }

  def add[M<:Data[M]:Manifest](edge: Sym[_], m: M): Unit = add(edge, m.key, m)

  def add[M<:Data[M]:Manifest](edge: Sym[_], m: Option[M]): Unit = m match {
    case Some(data) =>
      add(edge, data)
    case None =>
      remove(edge, keyOf[M])
  }
  def all(edge: Sym[_]): Iterator[(Class[_],Data[_])] = edge._data.toList.sortBy {case (k, _) => s"$k"}.iterator

  def remove(edge: Sym[_], key: Class[_]): Unit = {
    if (dbgPrint && (dbgPrintClasses contains key.getSimpleName)) {
      println(s"Removing: $edge[$key]")
    }
    edge._data.remove(key)
  }

  def clear[M<:Data[M]:Manifest](edge: Sym[_]): Unit = remove(edge, keyOf[M])

  def apply[M<:Data[M]:Manifest](edge: Sym[_]): Option[M] = edge._data.get(keyOf[M]).map(_.asInstanceOf[M])
}

@data object globals {
  def add[M<:Data[M]:Manifest](m: M): Unit = state.globals.add[M](m)
  def apply[M<:Data[M]:Manifest]: Option[M] = state.globals[M]
  def clear[M<:Data[M]:Manifest]: Unit = state.globals.clear[M]
  def invalidateBeforeTransform(): Unit = state.globals.invalidateBeforeTransform()

  def foreach(func: (Class[_],Data[_]) => Unit): Unit = state.globals.foreach(func)
}
