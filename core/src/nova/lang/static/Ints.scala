package nova.lang.static

import forge.tags._
import nova.core._
import nova.data.rangeOf

import scala.runtime.RichInt

trait LowerPriorityIntegerOps {
  implicit def int2RichInt(x: Int): RichInt = new RichInt(x)
}

trait LowPriorityIntegerOps extends LowerPriorityIntegerOps {
  //@api implicit def intToSeries(x: Int): Series = Series.alloc(Some(I32.c(x)),I32.c(x+1),None,None,isUnit=true)
  @api implicit def i32ToSeries(x: I32): Series = Series.alloc(start = x, end = x + I32.c(1), isUnit=true)

  @api implicit def IntToI32(x: Int): I32 = I32.c(x)
}

trait Ints extends LowPriorityIntegerOps {


  // Note: Naming is important here to override the name in Predef.scala
  // Note: Need the ctx and state at the implicit class to avoid issues with currying
  implicit class intWrapper(x: Int)(implicit ctx: SrcCtx, state: State) {
    def until(end: I32): Series = Series.alloc(start = I32.c(x), end = end)
    def by(step: I32): Series = Series.alloc(end = I32.c(x), step = step)
    def par(p: I32): Series = Series.alloc(end = I32.c(x), par = p)

    def until(end: Int): Series = Series.alloc(start = I32.c(x), end = I32.c(end))
    def by(step: Int): Series = Series.alloc(end = I32.c(x), step = I32.c(step))
    def par(p: Int): Series = Series.alloc(end = I32.c(x), par = I32.c(p))

    def ::(start: I32): Series = Series.alloc(start = start, end = I32.c(x))
    def ::(start: Int): Series = Series.alloc(start = I32.c(start), end = I32.c(x))

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

  @rig def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    rangeOf(p) = (start, stride, end)
    p
  }

}
