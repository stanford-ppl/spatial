package spatial.lang.api

import argon._
import forge.tags._

trait MiscAPI {
  def void: Void = Void.c

  def * = new Wildcard

  implicit class TextOps(t: Text) {
    @api def map[R:Type](f: U8 => R): Tensor1[R] = Tensor1.tabulate(t.length){i => f(t(i)) }

    @api def toCharArray: Tensor1[U8] = t.map{c => c}
  }

}
