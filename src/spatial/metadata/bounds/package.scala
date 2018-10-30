package spatial.metadata

import argon._
import emul.FixedPoint

package object bounds {

  implicit class BoundOps(s: Sym[_]) {
    def getBound: Option[Bound] = s match {
      case Literal(c: Int) => Some(Final(c))
      case Param(c: FixedPoint) if c.isExactInt & !metadata[SymbolBound](s).isDefined => Some(Expect(c.toInt))
      case _ => metadata[SymbolBound](s).map(_.bound)
    }
    def bound: Bound = getBound.getOrElse{ throw new Exception(s"Symbol $s was not bounded") }
    def bound_=(bnd: Bound): Unit = metadata.add(s, SymbolBound(bnd))

    def isGlobal: Boolean = s.isValue || metadata[Global](s).exists(_.flag)
    def isGlobal_=(flag: Boolean): Unit = metadata.add(s, Global(flag))

    def isFixedBits: Boolean = s.isValue || metadata[FixedBits](s).exists(_.flag)
    def isFixedBits_=(flag: Boolean): Unit = metadata.add(s, FixedBits(flag))
  }

}
