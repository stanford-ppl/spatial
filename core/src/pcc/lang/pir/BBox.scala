package pcc.lang
package pir

import forge._
import pcc.core._
import pcc.node.pir._

object BBox {
  @api def GEMM[T:Bits](a: T, b: T): T = stage(GEMMConfig(a,b))
  @api def GEMV[T:Bits](a: T, b: T): T = stage(GEMVConfig(a,b))
  @api def CONV[T:Bits](a: T, b: T): T = stage(CONVConfig(a,b))
}
