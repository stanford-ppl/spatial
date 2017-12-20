package pcc
package traversal

trait Transformer extends Pass {
  protected val f: Transformer = this
  final def apply[T](x: T): T = x
}
