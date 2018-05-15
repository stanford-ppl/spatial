package argon

import forge.tags._
import argon.transform.Transformer

import scala.collection.mutable


abstract class Data[T] { self =>
  final type Tx = Transformer

  /** If null, transformers will drop this metadata during mirroring/updating. */
  def mirror(f: Tx): T

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()

  /** Defines how to merge an old instance of this metadata with a mirrored instance.
    * The old metadata is metadata of this type already on the symbol.
    */
  def merge(old: T): Data[T] = self
  final def merge(old: Data[T]): Data[T] = merge(old.asInstanceOf[T])

  /** If true:
    *   Globals: Cleared PRIOR to transformation
    *   Symbols: Not explicitly transferred during symbol mirroring or updating
    */
  val ignoreOnTransform: Boolean = false
}

/** Globals: Persists across transformers (never dropped)
  * Symbols: Persists across transformers (never dropped)
  *
  * Primarily used for metadata which does not include symbols
  */
abstract class StableData[T] extends Data[T] {
  override def mirror(f:Tx) = this.asInstanceOf[T]
}

/** Globals: Cleared PRIOR to transformation.
  * Symbols: Not transferred explicitly during mirroring/updating.
  *
  * Primarily used for metadata set by flow rules
  */
abstract class FlowData[T] extends Data[T] {
  override def mirror(f:Tx): T = this.asInstanceOf[T]
  override val ignoreOnTransform: Boolean = true
}

/** Globals: Cleared AFTER transformation.
  * Symbols: Dropped during mirroring and updating.
  *
  * Primarily used for data which requires a new flow/analysis to run before it is valid again
  */
abstract class AnalysisData[T] extends Data[T] { self =>
  override def mirror(f: Tx): T = null.asInstanceOf[T]
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
  def mirrorAfterTransform(f: Transformer): Unit = state.globals.mirrorAfterTransform(f)
  def clearBeforeTransform(): Unit = state.globals.clearBeforeTransform()

  def foreach(func: (Class[_],Data[_]) => Unit): Unit = state.globals.foreach(func)
}
