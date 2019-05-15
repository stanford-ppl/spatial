package spatial.metadata

import argon._

package object rewrites {

  implicit class RewriteDataOps(s: Sym[_]) {
    def canFuseAsFMA: Boolean = metadata[CanFuseFMA](s).exists(_.canFuse)
    def canFuseAsFMA_=(canFuse: Boolean): Unit = metadata.add(s, CanFuseFMA(canFuse))
  }

}
