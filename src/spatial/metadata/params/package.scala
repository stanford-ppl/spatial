package spatial.metadata

import argon._
import spatial.lang.I32

package object params {

  implicit class ParamDomainOps(p: I32) {
    def getParamDomain: Option[(Int,Int,Int)] = metadata[ParamDomain](p).map{d => (d.min,d.step,d.max) }
    def paramDomain: (Int,Int,Int) = getParamDomain.getOrElse((1,1,1))
    def paramDomain_=(d: (Int,Int,Int)): Unit = metadata.add(p, ParamDomain(d._1,d._2,d._3))
  }

}
