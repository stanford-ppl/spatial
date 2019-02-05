package spatial.metadata

import argon._
import spatial.metadata.retiming._
import emul.ResidualGenerator._

package object math {

  implicit class MathOps(s: Sym[_]) {
    def getModulus: Option[Int] = metadata[Modulus](s).map(_.mod)
    def modulus: Int = getModulus.getOrElse(-1)
    def modulus_=(mod: Int): Unit = metadata.add(s, Modulus(mod))

    def getResidual: Option[ResidualGenerator] = metadata[Residual](s.trace).map(_.equ)
    def residual: ResidualGenerator = getResidual.getOrElse(if (s.trace.isConst) ResidualGenerator(s.traceToInt+1, s.traceToInt, s.traceToInt+1) else ResidualGenerator(1,0,0))
    def residual_=(equ: ResidualGenerator): Unit = metadata.add(s, Residual(equ))
  }

}
