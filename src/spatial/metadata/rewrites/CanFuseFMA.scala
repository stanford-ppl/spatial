package spatial.metadata.rewrites

import argon._

/** Metadata marking that an addition can be fused into a multiplication.
  * Getter:  sym.canFuseAsFMA
  * Setter:  sym.canFuseAsFMA = (true | false)
  * Default: false
  */
case class CanFuseFMA(canFuse: Boolean) extends Data[CanFuseFMA](transfer = Transfer.Mirror)
