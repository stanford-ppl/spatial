package argon

import argon.transform.Transformer

trait Mirrorable[A] {
  type Tx = Transformer
  def mirror(f:Tx): A
}
