package spatial.data

import argon._
import spatial.lang._

/** Minimum, step, and maximum DSE domain for a given design parameter.
  *
  * Option:  sym.getParamDomain
  * Getter:  sym.paramDomain : (min,step,max)
  * Setter:  sym.paramDomain = (min,step,max)
  * Default: min=1, max=1, step=1
  */
case class ParamDomain(min: Int, step: Int, max: Int) extends Data[ParamDomain](SetBy.User)


trait ParameterData {

  implicit class ParameterDataOps(p: I32) {
    def getParamDomain: Option[(Int,Int,Int)] = metadata[ParamDomain](p).map{d => (d.min,d.step,d.max) }
    def paramDomain: (Int,Int,Int) = getParamDomain.getOrElse((1,1,1))
    def paramDomain_=(d: (Int,Int,Int)): Unit = metadata.add(p, ParamDomain(d._1,d._2,d._3))
  }

}
