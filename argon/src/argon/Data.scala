package argon

import forge.tags._
import argon.transform.Transformer

import scala.collection.mutable

/** Any kind of IR graph metadata.
  *
  * DSL metadata instances should extend either StableData or AnalysisData.
  * StableData is for values that can persist safely across transformations.
  * AnalysisData is for any kind of metadata that needs updating during or after transformation.
  *
  * For consistency, global analysis data is dropped before transformers are run.
  */
sealed abstract class Data[T] { self =>
  final type Tx = Transformer

  /** Defines how to copy metadata during mirroring/updating. */
  def mirror(f: Tx): T = this.asInstanceOf[T]

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()

  /** Defines how to merge an old instance of this metadata with a mirrored instance.
    * The old metadata is metadata of this type already on the symbol.
    */
  def merge(old: T): Data[T] = self
  final def merge(old: Data[T]): Data[T] = merge(old.asInstanceOf[T])

  /** If true, metadata is:
    *   Globals: Invalidated BEFORE transformer runs
    *   Symbols: Invalidated (dropped) during mirroring / updating.
    */
  val invalidateOnTransform: Boolean = false
}

/** Globals: Persists across transformers (never dropped)
  * Symbols: Persists across transformers (never dropped)
  *
  * Primarily used for metadata which does not include symbols.
  * Use the merge method to define how to merge old and new instances.
  */
abstract class StableData[T] extends Data[T]

/** Metadata based only on node inputs (or functions thereof).
  * Note that subgraphs are considered node inputs here.
  *
  * Metadata is typically set when visiting the node it is defined on.
  *
  * Globals: Invalidated (dropped) BEFORE transformation.
  * Symbols: Invalidated (dropped) during mirroring / updating.
  *
  * Primarily used for metadata with symbols.
  */
abstract class InputData[T] extends Data[T] {
  override val invalidateOnTransform: Boolean = false
}

/** Metadata based on consumers of the node (or functions thereof).
  * Note that parents of subgraphs are considered node consumers here.
  *
  * Metadata is typically set when visiting node(s) that consume the output of that node.
  *
  * Globals: Invalidated (dropped) BEFORE transformation.
  * Symbols: Invalidated (dropped) during mirroring / updating.
  *
  * Primarily used for metadata with symbols.
  */
abstract class ConsumerData[T] extends Data[T] {
  override val invalidateOnTransform: Boolean = true
}

/** Global metadata which needs to be dropped before transformation.
  *
  * Globals: Invalidated (dropped) BEFORE transformation.
  */
abstract class GlobalData[T] extends Data[T] {
  override val invalidateOnTransform: Boolean = true
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
