package spatial.metadata

import argon._
import forge.tags.stateful
import emul.FixedPoint

package object bounds {

  implicit class BoundOps(s: Sym[_]) extends SpatialMetadata {
    @stateful def getBound: Option[Bound] = s match {
      case Literal(c: Int) => Some(Final(c))
      case _ if state.scratchpad[SymbolBound](s).isDefined => state.scratchpad[SymbolBound](s).map(_.bound)
      case Param(c: FixedPoint) if c.isExactInt & !metadata[SymbolBound](s).isDefined => Some(Expect(c.toInt))
      case _ => metadata[SymbolBound](s).map(_.bound)
    }
    @stateful def bound: Bound = getBound.getOrElse{ throw new Exception(s"Symbol $s was not bounded") }
    @stateful def bound_=(bnd: Bound): Unit = state.scratchpad.add(s, SymbolBound(bnd)) 

    @stateful def makeFinal(x: Int): Unit = {s.bound.isFinal = true; s.bound = Final(x)}

    def isGlobal: Boolean = s.isValue || metadata[Global](s).exists(_.flag)
    def isGlobal_=(flag: Boolean): Unit = metadata.add(s, Global(flag))

    def isFixedBits: Boolean = s.isValue || metadata[FixedBits](s).exists(_.flag)
    def isFixedBits_=(flag: Boolean): Unit = metadata.add(s, FixedBits(flag))

    def vecConst: Option[Seq[Any]] = metadata[VecConst](s).map { _.vs }
    def vecConst_=(vs: Seq[Any]): Unit = metadata.add(s, VecConst(vs))
  }

}
