package pcc.ir.static

import forge._
import pcc._
import pcc.data.domainOf

trait Params {

  implicit class ParamCreate(default: Int) {
    /**
      * Creates a parameter with this value as the default, and the given range with a stride of 1.
      *
      * ``1 (1 -> 5)``
      * creates a parameter with a default of 1 with a range [1,5].
      */
    @api def apply(range: (Int, Int))(implicit ov1: Overload0): I32 = createParam(default, range._1, 1, range._2)
    /**
      * Creates a parameter with this value as the default, and the given strided range.
      *
      * ``1 (1 -> 2 -> 8)``
      * creates a parameter with a default of 1 with a range in [2,8] with step of 4.
      */
    @api def apply(range: ((Int, Int), Int))(implicit ov2: Overload1): I32 = createParam(default, range._1._1, range._1._2, range._2)
  }

  @internal def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = param[I32](default)
    domainOf(p) = (start, stride, end)
    p
  }
}
