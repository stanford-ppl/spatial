package pcc.lang.static

import forge.api
import pcc.core._

trait Types {
  def bits[T:Bits]: Bits[T] = implicitly[Bits[T]]

  @api implicit def BooleanToBit(x: Boolean): Bit = const[Bit](x)
  @api implicit def UnitToVoid(x: Unit): Void = const[Void](x)

  @api implicit def FloatToF32(x: Float): F32 = const[F32](x)

  @api implicit def SeriesToCounter(x: Series): Counter = Counter(x.start,x.end,x.step,x.par)

}
