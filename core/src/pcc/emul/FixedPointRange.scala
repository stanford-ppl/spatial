package pcc.emul

import scala.collection.immutable.NumericRange

case class FixedPointRange(
  override val start: FixedPoint,
  override val end:   FixedPoint,
  override val step:  FixedPoint,
  override val isInclusive: Boolean
) extends NumericRange[FixedPoint](start,end,step,isInclusive)(FixedPoint.FixedPointIsIntegral) {

  override def copy(start: FixedPoint, end: FixedPoint, step: FixedPoint) = {
    FixedPointRange(start, end, step, isInclusive)
  }
}
