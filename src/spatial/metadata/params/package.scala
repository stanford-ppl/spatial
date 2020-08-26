package spatial.metadata

import argon._
import spatial.lang.I32
import forge.tags.stateful
import spatial.metadata.bounds._
import spatial.metadata.control._

package object params {

  implicit class ParamDomainOps(p: Sym[_]) {
    def getParamDomain: Option[Either[(Int,Int,Int), Seq[Int]]] = {
      if (getExplicitParamDomain.isDefined) Some(Right(explicitParamDomain))
      else if (getRangeParamDomain.isDefined) Some(Left(rangeParamDomain))
      else None
    }
    def paramDomain: Either[(Int,Int,Int), Seq[Int]] = getParamDomain.getOrElse(Left((1,1,1)))

    def getExplicitParamDomain: Option[Seq[Int]] = metadata[ExplicitParamDomain](p).map{d => d.values }
    def explicitParamDomain: Seq[Int] = getExplicitParamDomain.getOrElse(Seq(1))
    def explicitParamDomain_=(d: Seq[Int]): Unit = metadata.add(p, ExplicitParamDomain(d))

    def getRangeParamDomain: Option[(Int,Int,Int)] = metadata[RangeParamDomain](p).map{d => (d.min,d.step,d.max) }
    def rangeParamDomain: (Int,Int,Int) = getRangeParamDomain.getOrElse((1,1,1))
    def rangeParamDomain_=(d: (Int,Int,Int)): Unit = metadata.add(p, RangeParamDomain(d._1,d._2,d._3))

    def getContention: Option[Int] = metadata[MemoryContention](p).map{d => d.contention }
    def contention: Int = getContention.getOrElse(0)
    def contention_=(d: Int): Unit = metadata.add(p, MemoryContention(d))

    @stateful def getIntValue: Option[Int] = if (p.getBound.isDefined) Some(p.bound.toInt) else None
    @stateful def intValue: Int = p.bound.toInt
    @stateful def intValueOrLowest: Int = if (p.getBound.isDefined) p.bound.toInt else {p.paramDomain match {case Left((min,_,_)) => min; case Right(vals) => vals.sorted.head}}
    @stateful def setIntValue(d: Int): Unit = p.bound = Expect(d)
    @stateful def intValue_=(d: Int): Unit = p.bound = Expect(d)

    @stateful def makeFinalSched(d: CtrlSchedule): Unit = p.finalizeRawSchedule(d)
    @stateful def getSchedValue: Option[CtrlSchedule] = p.getRawSchedule
    @stateful def schedValue: CtrlSchedule = p.rawSchedule
    @stateful def setSchedValue(d: CtrlSchedule): Unit = p.rawSchedule = d
    @stateful def schedValue_=(d: CtrlSchedule): Unit = p.rawSchedule = d

    @stateful def getPrior: Prior = metadata[ParamPrior](p).map(_.prior).getOrElse(Uniform)
    @stateful def prior_=(pr: Prior) = metadata.add(p, ParamPrior(pr))

  }

  object Parameter {
    def unapply(x: Sym[_]): Option[Sym[_]] = x match {
      case x if x.isParam => Some(x)
      case _ => None
    }
  }

}
