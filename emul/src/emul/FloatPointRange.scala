package emul

import scala.collection.immutable.NumericRange

case class FloatPointRange(
  override val start: FloatPoint,
  override val end:   FloatPoint,
  override val step:  FloatPoint,
  override val isInclusive: Boolean
) extends NumericRange[FloatPoint](start,end,step,isInclusive)(FloatPoint.FloatPointIsIntegral) {

  override def copy(start: FloatPoint, end: FloatPoint, step: FloatPoint): FloatPointRange = {
    FloatPointRange(start, end, step, isInclusive)
  }
}

