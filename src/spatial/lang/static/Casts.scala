package spatial.lang
package static

import core._
import forge.tags._
import spatial.node._

trait CastsPriority2 {
  implicit def boxNum[A:Num](x: A): Num[A] = Num[A].box(x)

}

trait CastsPriority1 extends {
  implicit def boxBits[A:Bits](x: A): Bits[A] = Bits[A].box(x)
  implicit def boxOrder[A:Order](x: A): Order[A] = Order[A].box(x)
  implicit def boxArith[A:Arith](x: A): Arith[A] = Arith[A].box(x)
}

trait Casts extends CastsPriority1 { this: SpatialStatics =>
  implicit def boxSym[A:Type](x: A): Sym[A] = Type[A].boxed(x)

  implicit class CastType[A:Type](x: A) {
    @api def to[B](implicit cast: Cast[A,B]): B = cast.apply(x)
  }

  //=== Bit ===//
  class Cvt_Text_Bit extends Cast2Way[Text,Bit] {
    @rig def apply(x: Text): Bit = stage(TextToBit(x))
    @rig def applyLeft(x: Bit): Text = stage(BitToText(x))
  }
  implicit lazy val CastTextToBit: Cast[Text,Bit] = Right(new Cvt_Text_Bit)
  implicit lazy val CastBitToText: Cast[Bit,Text] = Left(new Cvt_Text_Bit)

  class Cvt_Bit_Fix[F:FixFmt] extends Cast2Way[Bit,Fix[F]] {
    @rig def apply(x: Bit): Fix[F] = mux(x, 1.to[Fix[F]], 0.to[Fix[F]])
    @rig def applyLeft(x: Fix[F]): Bit = x !== 0
  }
  implicit def CastBitToFix[F:FixFmt]: Cast[Bit,Fix[F]] = Right(new Cvt_Bit_Fix[F])
  implicit def CastFixToBit[F:FixFmt]: Cast[Fix[F],Bit] = Left(new Cvt_Bit_Fix[F])

  //=== Fix ===//

  class Cvt_Fix_Fix[F1:FixFmt,F2:FixFmt] extends CastFunc[Fix[F1],Fix[F2]] {
    @rig def apply(x: Fix[F1]): Fix[F2] = stage(FixToFix(x, FixFmt[F2]))
    @rig override def getLeft(x: Fix[F2]): Option[Fix[F1]] = Some(stage(FixToFix(x, FixFmt[F1])))
  }
  implicit def CastFixToFix[F1:FixFmt,F2:FixFmt]: Cast[Fix[F1],Fix[F2]] = Right(new Cvt_Fix_Fix[F1,F2])

  class Cvt_Text_Fix[F:FixFmt] extends Cast2Way[Text,Fix[F]] {
    @rig def apply(x: Text): Fix[F] = stage(TextToFix(x,FixFmt[F]))
    @rig def applyLeft(x: Fix[F]): Text = stage(FixToText(x))
  }
  implicit def CastTextToFix[F:FixFmt]: Cast[Text,Fix[F]] = Right(new Cvt_Text_Fix[F])
  implicit def CastFixToText[F:FixFmt]: Cast[Fix[F],Text] = Left(new Cvt_Text_Fix[F])

  //=== Flt ===//

  class Cvt_Flt_Flt[F1:FltFmt,F2:FltFmt] extends CastFunc[Flt[F1],Flt[F2]] {
    @rig def apply(x: Flt[F1]): Flt[F2] = stage(FltToFlt(x, FltFmt[F2]))
    @rig override def getLeft(x: Flt[F2]): Option[Flt[F1]] = Some(stage(FltToFlt(x, FltFmt[F1])))
  }
  implicit def CastFltToFlt[F1:FltFmt,F2:FltFmt]: Cast[Flt[F1],Flt[F2]] = Right(new Cvt_Flt_Flt[F1,F2])

  class Cvt_Text_Flt[F:FltFmt] extends Cast2Way[Text,Flt[F]] {
    @rig def apply(x: Text): Flt[F] = stage(TextToFlt(x,FltFmt[F]))
    @rig def applyLeft(x: Flt[F]): Text = stage(FltToText(x))
  }
  implicit def CastTextToFlt[F:FltFmt]: Cast[Text,Flt[F]] = Right(new Cvt_Text_Flt[F])
  implicit def CastFltToText[F:FltFmt]: Cast[Flt[F],Text] = Left(new Cvt_Text_Flt[F])


  class Cvt_Fix_Flt[F1:FixFmt,F2:FltFmt] extends Cast2Way[Fix[F1],Flt[F2]] {
    @rig def apply(a: Fix[F1]): Flt[F2] = stage(FixToFlt(a,FltFmt[F2]))
    @rig def applyLeft(b: Flt[F2]): Fix[F1] = stage(FltToFix(b,FixFmt[F1]))
  }
  implicit def CastFixToFlt[F1:FixFmt,F2:FltFmt]: Cast[Fix[F1],Flt[F2]] = Right(new Cvt_Fix_Flt[F1,F2])
  implicit def CastFltToFix[F1:FltFmt,F2:FixFmt]: Cast[Flt[F1],Fix[F2]] = Left(new Cvt_Fix_Flt[F2,F1])
}
