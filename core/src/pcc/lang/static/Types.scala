package pcc.lang.static

import forge.{SrcCtx, api}
import pcc.core._

trait Types {
  @api def void: Void = {
    implicit val ctx: SrcCtx = SrcCtx.empty
    Void.c
  }

  @api implicit def BooleanToBit(x: Boolean): Bit = const[Bit](x)
  @api implicit def UnitToVoid(x: Unit): Void = const[Void](x)

  @api implicit def FloatToF32(x: Float): F32 = const[F32](x)

  @api implicit def SeriesToCounter(x: Series): Counter = Counter.fromSeries(x)

}
