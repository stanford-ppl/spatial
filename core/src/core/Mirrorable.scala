package core

import core.transform.Transformer

trait Mirrorable[A] {
  type Tx = Transformer
  def mirror(f:Tx): A
}
