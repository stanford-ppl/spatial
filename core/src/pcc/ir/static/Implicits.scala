package pcc.ir.static

import forge._
import pcc._

trait Implicits {
  @api implicit def IntToI32(x: Int): I32 = const[I32](x)
  @api implicit def FloatToF32(x: Float): F32 = const[F32](x)

  @api implicit def SeriesToCounter(x: Series): Counter = Counter(x.start,x.end,x.step,x.par)
}
