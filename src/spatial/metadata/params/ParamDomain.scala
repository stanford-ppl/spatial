package spatial.metadata.params

import argon._
import spatial.metadata.control._

/** Minimum, step, and maximum DSE domain for a given design parameter.
  *
  * Option:  sym.getParamDomain
  * Getter:  sym.paramDomain : (min,step,max)
  * Setter:  sym.paramDomain = (min,step,max)
  * Default: min=1, max=1, step=1
  */
case class ParamDomain(min: Int, step: Int, max: Int) extends Data[ParamDomain](SetBy.User)

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