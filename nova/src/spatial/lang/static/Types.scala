package spatial.lang.static

import forge.tags._

trait Types {
  @api def void: Void = Void.c

  @api implicit def BooleanToBit(x: Boolean): Bit = Bit.c(x)
  @api implicit def UnitToVoid(x: Unit): Void = Void.c
  @api implicit def FloatToF32(x: Float): F32 = F32.c(x)
  @api implicit def DoubleToF32(x: Double): F32 = F32.c(x.toFloat)  // FIXME

  @api implicit def SeriesToCounter(x: Series): Counter = Counter.fromSeries(x)
}
