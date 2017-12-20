package pcc.core

import pcc.traversal.Transformer

abstract class Metadata[T] { self =>
  type Tx = Transformer

  /** If null, transformers will drop this metadata during mirroring. **/
  def mirror(f: Tx): T

  final def key: Class[_] = self.getClass
  override final def hashCode(): Int = key.hashCode()
}

abstract class SimpleData[T] extends Metadata[T] {
  override def mirror(f:Tx): T = this.asInstanceOf[T]
}

abstract class ComplexData[T] extends Metadata[T] { self =>
  override def mirror(f: Tx): T = null.asInstanceOf[T]
}
