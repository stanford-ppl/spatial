package spatial.metadata.params

import argon._
import spatial.metadata.control._

/** Minimum, step, and maximum DSE domain for a given design parameter.
  *
  * Option:  sym.getRangeParamDomain
  * Getter:  sym.rangeParamDomain : (min,step,max)
  * Setter:  sym.rangeParamDomain = (min,step,max)
  * Default: min=1, max=1, step=1
  */
case class RangeParamDomain(min: Int, step: Int, max: Int) extends Data[RangeParamDomain](SetBy.User)

/** All possible values for a design parameter.
  *
  * Option:  sym.getExplicitParamDomain
  * Getter:  sym.explicitParamDomain : Seq[Int]
  * Setter:  sym.explicitParamDomain = Seq[Int]
  * Default: Seq(1)
  */
case class ExplicitParamDomain(values: Seq[Int]) extends Data[ExplicitParamDomain](SetBy.User)

/** Value for a given design parameter.
  *
  * Getter:  sym.getIntValue : v
  * Getter:  sym.intValue(state) : v
  * Setter:  sym.setIntValue(v)(state)
  * Setter:  sym.intValue = v
  * Default: 1
  */
case class IntParamValue(v: Int) extends Data[IntParamValue](SetBy.Analysis.Self)

/** Value for a given design parameter.
  *
  * Getter:  sym.getSchedValue : v
  * Getter:  sym.schedValue(state) : v
  * Setter:  sym.setSchedValue(v)(state)
  * Setter:  sym.schedValue = v
  * Default: 1
  */
case class SchedParamValue(v: CtrlSchedule) extends Data[SchedParamValue](SetBy.Analysis.Self)

case class ParamPrior(prior: Prior) extends Data[ParamPrior](SetBy.Analysis.Self)

