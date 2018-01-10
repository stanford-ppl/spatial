package pcc
package traversal
package transform

case class IdentityTransform(IR: State) extends ForwardTransformer {
  override val name = "Identity Transform"
}
