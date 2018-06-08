package spatial.data

import argon._

/** Metadata marking that an addition can be fused into a multiplication.
  * Getter:  sym.canFuseAsFMA
  * Setter:  sym.canFuseAsFMA = (true | false)
  * Default: false
  */
case class CanFuseFMA(canFuse: Boolean) extends Data[CanFuseFMA](transfer = Transfer.Mirror)

trait RewriteData {
  implicit class RewriteDataOps(s: Sym[_]) {
    def canFuseAsFMA: Boolean = metadata[CanFuseFMA](s).exists(_.canFuse)
    def canFuseAsFMA_=(canFuse: Boolean): Unit = metadata.add(s, CanFuseFMA(true))
  }
}
