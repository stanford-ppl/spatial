package spatial.metadata.params

import argon._

/** Minimum, step, and maximum DSE domain for a given design parameter.
  *
  * Option:  sym.getParamDomain
  * Getter:  sym.paramDomain : (min,step,max)
  * Setter:  sym.paramDomain = (min,step,max)
  * Default: min=1, max=1, step=1
  */
case class ParamDomain(min: Int, step: Int, max: Int) extends Data[ParamDomain](SetBy.User)

