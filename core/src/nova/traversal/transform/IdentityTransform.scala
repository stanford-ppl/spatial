package nova.traversal
package transform

import nova.core._

case class IdentityTransform(IR: State) extends ForwardTransformer
