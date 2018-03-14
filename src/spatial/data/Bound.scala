package spatial.data

import forge.tags._
import core._
import emul.FixedPoint

// TODO[2]: Bound is in terms of Int right now?
abstract class Bound(x: Int) { def toInt: Int = x }
case class Final(x: Int) extends Bound(x)
case class Expect(x: Int) extends Bound(x)

case class SymbolBound(bound: Bound) extends FlowData[SymbolBound]
object boundOf {
  def get(x: Sym[_]): Option[Bound] = x match {
    case Literal(c: Int) => Some(Final(c))
    case Param(c: FixedPoint) if c.isExactInt => Some(Expect(c.toInt))
    case _ => metadata[SymbolBound](x).map(_.bound)
  }
  def apply(x: Sym[_]): Bound = boundOf.get(x).getOrElse(throw new Exception(s"Symbol $x was not bounded"))

  def update(x: Sym[_], bnd: Bound): Unit = metadata.add(x, SymbolBound(bnd))
}

object Final {
  def unapply(x: Bound): Option[Int] = x match {
    case f: Final => Some(f.x)
    case _ => None
  }
  def unapply(x: Sym[_]): Option[Int] = boundOf.get(x) match {
    case Some(x: Final) => Some(x.toInt)
    case _ => None
  }
}

object Expect {
  def unapply(x: Bound): Option[Int] = Some(x.toInt)
  def unapply(x: Sym[_]): Option[Int] = boundOf.get(x).map(_.toInt)
}
