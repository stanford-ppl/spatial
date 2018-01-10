package pcc.ir.static

import forge._
import pcc._
import pcc.data.domainOf

import scala.runtime.RichInt

trait LowerPriorityIntegerOps {
  implicit def int2RichInt(x: Int): RichInt = new RichInt(x)
}

trait LowPriorityIntegerOps extends LowerPriorityIntegerOps {
  //@api implicit def intToSeries(x: Int): Series = Series.alloc(Some(I32.c(x)),I32.c(x+1),None,None,isUnit=true)
  @api implicit def i32ToSeries(x: I32): Series = Series.alloc(Some(x),x + I32.c(1), None, None, isUnit=true)
}

trait Ints extends LowPriorityIntegerOps {

  @api implicit def IntToI32(x: Int): I32 = I32.c(x)

  // Note: Naming is important here to override the name in Predef.scala
  // Note: Need the ctx and state at the implicit class to avoid issues with currying
  implicit class intWrapper(x: Int)(implicit ctx: SrcCtx, state: State) {
    def until(end: I32): Series = Series.alloc(Some(I32.c(x)), end, None, None)
    def by(step: I32): Series = Series.alloc(None,I32.c(x),Some(step),None)
    def par(p: I32): Series = Series.alloc(None, I32.c(x), None, Some(p))

    def until(end: Int): Series = Series.alloc(Some(I32.c(x)), I32.c(end), None, None)
    def by(step: Int): Series = Series.alloc(None, I32.c(x), Some(I32.c(step)), None)
    def par(p: Int): Series = Series.alloc(None, I32.c(x), None, Some(I32.c(p)))

    def ::(start: I32): Series = Series.alloc(Some(start), I32.c(x), None, None)
    def ::(start: Int): Series = Series.alloc(Some(I32.c(start)), I32.c(x), None, None)

    /**
      * Creates a parameter with this value as the default, and the given range with a stride of 1.
      *
      * ``1 (1 -> 5)``
      * creates a parameter with a default of 1 with a range [1,5].
      */
    def apply(range: (Int, Int))(implicit ov1: Overload0): I32 = createParam(x, range._1, 1, range._2)
    /**
      * Creates a parameter with this value as the default, and the given strided range.
      *
      * ``1 (1 -> 2 -> 8)``
      * creates a parameter with a default of 1 with a range in [2,8] with step of 4.
      */
    def apply(range: ((Int, Int), Int))(implicit ov2: Overload1): I32 = createParam(x, range._1._1, range._1._2, range._2)


    //def to[B:Type](implicit cast: Cast[Int,B]): B = cast(x)
  }

  @internal def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    domainOf(p) = (start, stride, end)
    p
  }

}
