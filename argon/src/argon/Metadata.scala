package argon

import forge.tags._
import argon.transform.Transformer

import scala.collection.mutable


abstract class Metadata[T] { self =>
  final type Tx = Transformer

  /** If null, transformers will drop this metadata during mirroring/updating. */
  def mirror(f: Tx): T

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()

  /** If true:
    *   Globals: Cleared PRIOR to transformation
    *   Symbols: Dropped during symbol mirroring (but not updating)
    */
  val skipOnTransform: Boolean = false
}

/** Globals: Persists across transformers (never dropped)
  * Symbols: Persists across transformers (never dropped)
  *
  * Primarily used for metadata which does not include symbols
  */
abstract class StableData[T] extends Metadata[T] {
  override def mirror(f:Tx) = this.asInstanceOf[T]
}

/** Globals: Cleared PRIOR to transformation.
  * Symbols: Dropped during mirroring but not updating.
  *
  * Primarily used for metadata set by flow rules
  */
abstract class FlowData[T] extends Metadata[T] {
  override def mirror(f:Tx): T = this.asInstanceOf[T]
  override val skipOnTransform: Boolean = true
}

/** Globals: Cleared AFTER transformation.
  * Symbols: Dropped during mirroring and updating.
  *
  * Primarily used for data which requires a new flow/analysis to run before it is valid again
  */
abstract class AnalysisData[T] extends Metadata[T] { self =>
  override def mirror(f: Tx): T = null.asInstanceOf[T]
}

/** Shortcuts for metadata */
object metadata {
  type Data = mutable.Map[Class[_],Metadata[_]]

  private def keyOf[M<:Metadata[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  def addAll(edge: Sym[_], data: Iterator[Metadata[_]]): Unit = data.foreach{m => edge._data += (m.key -> m) }
  def addOrRemoveAll(edge: Sym[_], data: Iterator[(Class[_],Option[Metadata[_]])]): Unit = data.foreach{
    case (key,Some(m)) => edge._data += (key -> m)
    case (key,None)    => edge._data.remove(key)
  }

  def add[M<:Metadata[M]:Manifest](edge: Sym[_], m: M): Unit = edge._data += (m.key -> m)
  def add[M<:Metadata[M]:Manifest](edge: Sym[_], m: Option[M]): Unit = m match {
    case Some(data) => edge._data += (data.key -> data)
    case None => edge._data.remove(keyOf[M])
  }
  def all(edge: Sym[_]): Iterator[(Class[_],Metadata[_])] = edge._data.iterator

  def clear[M<:Metadata[M]:Manifest](edge: Sym[_]): Unit = edge._data.remove(keyOf[M])

  def apply[M<:Metadata[M]:Manifest](edge: Sym[_]): Option[M] = edge._data.get(keyOf[M]).map(_.asInstanceOf[M])
}

@data object globals {
  def add[M<:Metadata[M]:Manifest](m: M): Unit = state.globals.add[M](m)
  def apply[M<:Metadata[M]:Manifest]: Option[M] = state.globals[M]
  def clear[M<:Metadata[M]:Manifest]: Unit = state.globals.clear[M]
  def mirrorAfterTransform(f: Transformer): Unit = state.globals.mirrorAfterTransform(f)
  def clearBeforeTransform(): Unit = state.globals.clearBeforeTransform()

  def foreach(func: (Class[_],Metadata[_]) => Unit): Unit = state.globals.foreach(func)
}
