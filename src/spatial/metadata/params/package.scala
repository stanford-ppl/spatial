package spatial.metadata

import argon._
import spatial.lang.I32
import forge.tags.stateful
import spatial.metadata.control._

package object params {

  implicit class ParamDomainOps(p: Sym[_]) {
    def getParamDomain: Option[(Int,Int,Int)] = metadata[ParamDomain](p).map{d => (d.min,d.step,d.max) }
    def paramDomain: (Int,Int,Int) = getParamDomain.getOrElse((1,1,1))
    def paramDomain_=(d: (Int,Int,Int)): Unit = metadata.add(p, ParamDomain(d._1,d._2,d._3))

    def getContention: Option[Int] = metadata[MemoryContention](p).map{d => d.contention }
    def contention: Int = getContention.getOrElse(0)
    def contention_=(d: Int): Unit = metadata.add(p, MemoryContention(d))

    def getIntValue: Option[Int] = metadata[IntParamValue](p).map{d => d.v }
    @stateful def intValue: Int = getIntValue.getOrElse(1)
    @stateful def setIntValue(d: Int): Unit = metadata.add(p, IntParamValue(d))
    def intValue_=(d: Int): Unit = metadata.add(p, IntParamValue(d))

    def getSchedValue: Option[CtrlSchedule] = metadata[SchedParamValue](p).map{d => d.v }
    @stateful def schedValue: CtrlSchedule = getSchedValue.getOrElse(p.schedule)
    @stateful def setSchedValue(d: CtrlSchedule): Unit = metadata.add(p, SchedParamValue(d))
    def schedValue_=(d: CtrlSchedule): Unit = metadata.add(p, SchedParamValue(d))
  }

  object Parameter {
    def unapply(x: Sym[_]): Option[Sym[_]] = x match {
      case x if x.isParam => Some(x)
      case _ => None
    }
  }

}
