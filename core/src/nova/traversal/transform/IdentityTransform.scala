package nova.traversal
package transform

import nova.core._

case class IdentityTransform(IR: State) extends ForwardTransformer {
  override val name = "Identity Transform"
}
