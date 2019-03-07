package argon

import argon.transform.Transformer

trait Mirrorable[A] extends Serializable {
  type Tx = Transformer
  def mirror(f:Tx): A
}
