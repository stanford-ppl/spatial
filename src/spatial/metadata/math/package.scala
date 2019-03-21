package spatial.metadata

import argon._
import spatial.lang._
import spatial.metadata.bounds._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.metadata.control._
import emul.ResidualGenerator._
import forge.tags.stateful

import utils.math.{isPow2,log2}

package object math {

  implicit class MathOps(s: Sym[_]) {
    def getModulus: Option[Int] = metadata[Modulus](s).map(_.mod)
    def modulus: Int = getModulus.getOrElse(-1)
    def modulus_=(mod: Int): Unit = metadata.add(s, Modulus(mod))

    def getResidual: Option[ResidualGenerator] = metadata[Residual](s.trace).map(_.equ)
    @stateful def residual: ResidualGenerator = getResidual.getOrElse(
    	if (s.trace.isConst) ResidualGenerator(s.traceToInt) 
    	else if (s.trace.asInstanceOf[Num[_]].getCounter.isDefined && s.trace.asInstanceOf[Num[_]].counter.ctr.isStaticStartAndStep) {
	      val Final(start) = s.trace.asInstanceOf[Num[_]].counter.ctr.start
	      val Final(step) = s.trace.asInstanceOf[Num[_]].counter.ctr.step
	      val par = s.trace.asInstanceOf[Num[_]].counter.ctr.ctrPar.toInt
	      val lanes = s.trace.asInstanceOf[Num[_]].counter.lanes
	      val A = par * step
        val B = lanes.map { lane => start + lane * step }
	      ResidualGenerator(A, B, 0)
    	}
    	else ResidualGenerator(1,0,0))
    def residual_=(equ: ResidualGenerator): Unit = metadata.add(s, Residual(equ))
  }

}
