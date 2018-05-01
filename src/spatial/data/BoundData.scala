package spatial.data

import argon._
import emul.FixedPoint

// TODO[2]: Bound is in terms of Int right now?
abstract class Bound(x: Int) { def toInt: Int = x }
case class Final(x: Int) extends Bound(x)
case class Expect(x: Int) extends Bound(x)
case class UpperBound(x: Int) extends Bound(x)

case class SymbolBound(bound: Bound) extends FlowData[SymbolBound]

case class Global(flag: Boolean) extends FlowData[Global]


object Final {
  def unapply(x: Bound): Option[Int] = x match {
    case f: Final => Some(f.x)
    case _ => None
  }
  def unapply(x: Sym[_]): Option[Int] = x.getBound match {
    case Some(x: Final) => Some(x.toInt)
    case _ => None
  }
}

object Expect {
  def unapply(x: Bound): Option[Int] = Some(x.toInt)
  def unapply(x: Sym[_]): Option[Int] = x.getBound.map(_.toInt)
}

object Upper {
  def unapply(x: Sym[_]): Option[Int] = x.getBound.map(_.toInt)
}



trait BoundData {

  implicit class BoundOps(s: Sym[_]) {
    def getBound: Option[Bound] = s match {
      case Literal(c: Int) => Some(Final(c))
      case Param(c: FixedPoint) if c.isExactInt => Some(Expect(c.toInt))
      case _ => metadata[SymbolBound](s).map(_.bound)
    }
    def bound: Bound = getBound.getOrElse{ throw new Exception(s"Symbol $s was not bounded") }
    def bound_=(bnd: Bound): Unit = metadata.add(s, SymbolBound(bnd))


    def isGlobal: Boolean = s.isValue || metadata[Global](s).exists(_.flag)
    def isGlobal_=(flag: Boolean): Unit = metadata.add(s, Global(flag))
  }


}