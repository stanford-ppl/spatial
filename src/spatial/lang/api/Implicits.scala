package spatial.lang.api

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.params._
import utils.Overloads._

trait Implicits extends argon.lang.api.Implicits { this: MuxAPI =>

  class Cvt_Bit_Fix[S:BOOL,I:INT,F:INT] extends Cast2Way[Bit,Fix[S,I,F]] {
    @rig def apply(x: Bit): Fix[S,I,F] = mux(x, 1.to[Fix[S,I,F]], 0.to[Fix[S,I,F]])
    @rig def applyLeft(x: Fix[S,I,F]): Bit = x !== 0
  }
  implicit def CastBitToFix[S:BOOL,I:INT,F:INT]: Cast[Bit,Fix[S,I,F]] = Right(new Cvt_Bit_Fix[S,I,F])
  implicit def CastFixToBit[S:BOOL,I:INT,F:INT]: Cast[Fix[S,I,F],Bit] = Left(new Cvt_Bit_Fix[S,I,F])

  implicit class IntParameters(b: Int)(implicit ctx: SrcCtx, state: State) {
    /**
      * Creates a parameter with this value as the default, and the given range with a stride of 1.
      *
      * ``1 (1 -> 5)``
      * creates a parameter with a default of 1 with a range [1,5].
      */
    def apply(range: (Int, Int))(implicit ov1: Overload0): I32 = createParam(b, range._1, 1, range._2)
    /**
      * Creates a parameter with this value as the default, and the given strided range.
      *
      * ``1 (1 -> 2 -> 8)``
      * creates a parameter with a default of 1 with a range in [2,8] with step of 4.
      */
    def apply(range: ((Int, Int), Int))(implicit ov2: Overload1): I32 = createParam(b, range._1._1, range._1._2, range._2)
    /**
      * Creates a parameter with this value as the default, and the given alternatives.
      *
      * ``1 (1,2,4,8,16)``
      * creates a parameter with a default of 1 with possible alternative values of 2, 4, 8, and 16.
      */
    def apply(possibilities: Int*)(implicit ov2: Overload1): I32 = createParam(b, possibilities)
  }

  @api def param[A](c: Lift[A]): A = {
    c.B.from(c.literal, errorOnLoss = true, isParam = true)
  }

  @rig def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    p.rangeParamDomain = (start, stride, end)
    p
  }

  @rig def createParam(default: Int, possible: Seq[Int]): I32 = {
    val p = I32.p(default)
    p.explicitParamDomain = possible
    p
  }



  // ---------------------------------- Spatial Types ------------------------------------------- //
  // --- Reg[A]
  class RegNumerics[A:Num](reg: Reg[A])(implicit ctx: SrcCtx, state: State) {
    def :+=(data: A): Void = reg := reg.value + data.unbox
    def :-=(data: A): Void = reg := reg.value - data.unbox
    def :*=(data: A): Void = reg := reg.value * data.unbox
  }

  @api implicit def regRead[A](x: Reg[A]): A = x.value
  @api implicit def regNumerics[A:Num](x: Reg[A]): RegNumerics[A] = new RegNumerics[A](x)


  // --- Wildcard
  @api implicit def wildcardToForever(w: Wildcard): Counter[ICTR] = stage(ForeverNew())


  // --- Series
  @api implicit def SeriesToCounter[S:BOOL,I:INT,F:INT](x: Series[Fix[S,I,F]]): Counter[Fix[S,I,F]] = Counter.from(x)

}
