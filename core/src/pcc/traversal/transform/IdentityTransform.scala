package pcc.traversal
package transform

import pcc.core._

case class IdentityTransform(IR: State) extends ForwardTransformer {
  override val name = "Identity Transform"
}
