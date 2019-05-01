package spatial.metadata.bounds

import argon._
import spatial.metadata.params._
import forge.tags.stateful
import spatial.metadata.SpatialMetadata

// TODO[2]: Bound is in terms of Int right now?
abstract class Bound(x: Int) extends SpatialMetadata { 
  def toInt: Int = x 

  var isFinal: Boolean = false

  def meet(that: Bound): Bound = {
    if (this.isInstanceOf[Expect] && that.isInstanceOf[Expect])
      Expect(x max that.toInt)
    else if (this.isInstanceOf[Final] && that.isInstanceOf[Final])
      Final(x max that.toInt)
    else UpperBound(x max that.toInt)
  }
}
case class Final(x: Int) extends Bound(x) 
case class Expect(x: Int) extends Bound(x)
case class UpperBound(x: Int) extends Bound(x)

/** Defines the upper bound value of a symbol, if any.
  *
  * Option:  sym.getBound
  * Getter:  sym.bound
  * Setter:  sym.bound = (Bound)
  * Default: undefined
  *
  * Matchers: Final(value: Int)  - for exact values
  *           Expect(value: Int) - for, e.g. functions of unfinalized parameters
  *           Upper(value: Int)  - for upper bounds (usually set by user)
  */
case class SymbolBound(bound: Bound) extends Data[SymbolBound](SetBy.Analysis.Self)

/** Flags that a symbol is a "global".
  * In Spatial, a "global" is any value which is solely a function of input arguments
  * and constants. These are computed prior to starting the main computation, and
  * therefore appear constant to the majority of the program.
  *
  * Getter:  sym.isFixedBits
  * Setter:  sym.isFixedBits = (true|false)
  * Default: false
  */
case class Global(flag: Boolean) extends Data[Global](SetBy.Flow.Self)

/** Flags that a symbol is representable as a statically known list of bits.
  *
  * Getter:  sym.isFixedBits
  * Setter:  sym.isFixedBits = (true|false)
  * Default: false
  */
case class FixedBits(flag: Boolean) extends Data[FixedBits](SetBy.Flow.Self)


object Final {
  @stateful def unapply(x: Bound): Option[Int] = x match {
    case f: Final => Some(f.x)
    // case f: Expect if f.isFinal => x.getIntValue //TODO
    case _ => None
  }
  @stateful def unapply(x: Sym[_]): Option[Int] = x.getBound match {
    case Some(y: Final) => Some(y.toInt)
    case Some(y: Expect) if y.isFinal => x.getIntValue
    case _ => None
  }
}

object Expect {
  @stateful def unapply(x: Bound): Option[Int] = Some(x.toInt)
  @stateful def unapply(x: Sym[_]): Option[Int] = if (x.getIntValue.isDefined) x.getIntValue else x.getBound.map(_.toInt)
}

object Upper {
  @stateful def unapply(x: Sym[_]): Option[Int] = x.getBound match {
    case Some(y: UpperBound) => Some(y.toInt)
    case _ => None
  }
}

object Bounded {
  @stateful def unapply(x: Sym[_]): Option[Bound] = x.getBound
}

/*
 * Metadata set on bound indicating the const value contains actually a vector 
 * of values as supposed to be a single one. Used in vectorizing inner loop
 * for PIR where iterators can be a vector of constant
 * */
case class VecConst(vs: Seq[Any]) extends Data[VecConst](SetBy.Analysis.Self)
object VecConst {
  def unapply(x:Any) = x match {
    case x:Sym[_] if x.vecConst.nonEmpty => Some(x.vecConst.get)
    case x => None
  }
}
