package spatial.lang
package static

import core._
import forge.tags._
import spatial.node._

trait CastsPriority3 {

  implicit class AppendOps[A:Type](x: A)(implicit ctx: SrcCtx, state: State) {
    def +(y: Any): Text = (x, y) match {
      case (a: Top[_], b: Top[_]) => a.toText ++ b.toText
      case (a, b: Top[_]) => Text(a.toString) ++ b.toText
      case (a: Top[_], b) => a.toText ++ Text(b.toString)
      case (a, b)         => Text(a.toString) ++ Text(b.toString)
    }
  }

}

trait CastsPriority2 extends CastsPriority3 {
  implicit def boxNum[A:Num](x: A): Num[A] = Num[A].box(x)
}

trait CastsPriority1 extends CastsPriority2 {
  implicit def boxBits[A:Bits](x: A): Bits[A] = Bits[A].box(x)
  implicit def boxOrder[A:Order](x: A): Order[A] = Order[A].box(x)
  implicit def boxArith[A:Arith](x: A): Arith[A] = Arith[A].box(x)

  implicit def selfCast[A:Type]: Cast[A,A] = Right(new CastFunc[A,A] {
    @rig def apply(a: A): A = a
  })
}

trait Casts extends CastsPriority1 { this: SpatialStatics =>
  implicit def boxTop[A:Type](x: A): Top[A] = Type[A].boxed(x).asInstanceOf[Top[A]]
  implicit class BoxSym[A:Type](x: A) extends core.static.ExpMiscOps[Any,A](x)


  @api implicit def regRead[A:Bits](x: Reg[A]): A = x.value
  @api implicit def argRead[A:Bits](x: ArgIn[A]): A = x.value


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

  class Cvt_Bit_Fix[S:BOOL,I:INT,F:INT] extends Cast2Way[Bit,Fix[S,I,F]] {
    @rig def apply(x: Bit): Fix[S,I,F] = mux(x, 1.to[Fix[S,I,F]], 0.to[Fix[S,I,F]])
    @rig def applyLeft(x: Fix[S,I,F]): Bit = x !== 0
  }
  implicit def CastBitToFix[S:BOOL,I:INT,F:INT]: Cast[Bit,Fix[S,I,F]] = Right(new Cvt_Bit_Fix[S,I,F])
  implicit def CastFixToBit[S:BOOL,I:INT,F:INT]: Cast[Fix[S,I,F],Bit] = Left(new Cvt_Bit_Fix[S,I,F])

  //=== Fix ===//

  class Cvt_Fix_Fix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT] extends CastFunc[Fix[S1,I1,F1],Fix[S2,I2,F2]] {
    @rig def apply(x: Fix[S1,I1,F1]): Fix[S2,I2,F2] = stage(FixToFix(x, FixFmt.from[S2,I2,F2]))
    @rig override def getLeft(x: Fix[S2,I2,F2]): Option[Fix[S1,I1,F1]] = Some(stage(FixToFix(x, FixFmt.from[S1,I1,F1])))
  }
  implicit def CastFixToFix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT]: Cast[Fix[S1,I1,F1],Fix[S2,I2,F2]] = {
    Right(new Cvt_Fix_Fix[S1,I1,F1,S2,I2,F2])
  }

  class Cvt_Text_Fix[S:BOOL,I:INT,F:INT] extends Cast2Way[Text,Fix[S,I,F]] {
    @rig def apply(x: Text): Fix[S,I,F] = stage(TextToFix(x,FixFmt.from[S,I,F]))
    @rig def applyLeft(x: Fix[S,I,F]): Text = stage(FixToText(x))
  }
  implicit def CastTextToFix[S:BOOL,I:INT,F:INT]: Cast[Text,Fix[S,I,F]] = Right(new Cvt_Text_Fix[S,I,F])
  implicit def CastFixToText[S:BOOL,I:INT,F:INT]: Cast[Fix[S,I,F],Text] = Left(new Cvt_Text_Fix[S,I,F])

  //=== Flt ===//

  class Cvt_Flt_Flt[M1:INT,E1:INT,M2:INT,E2:INT] extends CastFunc[Flt[M1,E1],Flt[M2,E2]] {
    @rig def apply(x: Flt[M1,E1]): Flt[M2,E2] = stage(FltToFlt(x, FltFmt.from[M2,E2]))
    @rig override def getLeft(x: Flt[M2,E2]): Option[Flt[M1,E1]] = Some(stage(FltToFlt(x, FltFmt.from[M1,E1])))
  }
  implicit def CastFltToFlt[M1:INT,E1:INT,M2:INT,E2:INT]: Cast[Flt[M1,E1],Flt[M2,E2]] = {
    Right(new Cvt_Flt_Flt[M1,E1,M2,E2])
  }

  class Cvt_Text_Flt[M:INT,E:INT] extends Cast2Way[Text,Flt[M,E]] {
    @rig def apply(x: Text): Flt[M,E] = stage(TextToFlt(x,FltFmt.from[M,E]))
    @rig def applyLeft(x: Flt[M,E]): Text = stage(FltToText(x))
  }
  implicit def CastTextToFlt[M:INT,E:INT]: Cast[Text,Flt[M,E]] = Right(new Cvt_Text_Flt[M,E])
  implicit def CastFltToText[M:INT,E:INT]: Cast[Flt[M,E],Text] = Left(new Cvt_Text_Flt[M,E])


  class Cvt_Fix_Flt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT] extends Cast2Way[Fix[S1,I1,F1],Flt[M2,E2]] {
    @rig def apply(a: Fix[S1,I1,F1]): Flt[M2,E2] = stage(FixToFlt(a,FltFmt.from[M2,E2]))
    @rig def applyLeft(b: Flt[M2,E2]): Fix[S1,I1,F1] = stage(FltToFix(b,FixFmt.from[S1,I1,F1]))
  }
  implicit def CastFixToFlt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT]: Cast[Fix[S1,I1,F1],Flt[M2,E2]] = Right(new Cvt_Fix_Flt[S1,I1,F1,M2,E2])
  implicit def CastFltToFix[M1:INT,E1:INT,S2:BOOL,I2:INT,F2:INT]: Cast[Flt[M1,E1],Fix[S2,I2,F2]] = Left(new Cvt_Fix_Flt[S2,I2,F2,M1,E1])
}
