package pcc.core

import pcc.traversal.transform.Transformer

abstract class Metadata[T] { self =>
  final type Tx = Transformer

  /** If null, transformers will drop this metadata during mirroring. **/
  def mirror(f: Tx): T

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()

  val clearBeforeTransform: Boolean = false
}

/**
  * Once set, persists across transformers and during mirroring
  * Primarily used for metadata which does not include symbols
  */
abstract class StableData[T] extends Metadata[T] {
  override def mirror(f:Tx) = this.asInstanceOf[T]
}

/**
  * Removed before transformer runs, but kept during mirroring
  * Primarily used for metadata set by flow rules
  */
abstract class FlowData[T] extends Metadata[T] {
  override def mirror(f:Tx): T = this.asInstanceOf[T]
  override val clearBeforeTransform: Boolean = true
}

/**
  * Removed during/after transformer has run
  * Primarily used for data which requires a new analysis to run before it is valid again
  */
abstract class AnalysisData[T] extends Metadata[T] { self =>
  override def mirror(f: Tx): T = null.asInstanceOf[T]
}
