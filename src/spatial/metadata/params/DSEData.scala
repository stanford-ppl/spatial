package spatial.metadata.params

import argon._
import forge.tags.data
import forge.tags.stateful
import utils.recursive._
import spatial.metadata.types._

/** Global set of all dse params to ignore.
  *
  * Getter:  IgnoreParams.all
  * Append:  IgnoreParams += (mem)
  * Default: empty set
  */
case class IgnoreParams(params: Set[Sym[_]]) extends Data[IgnoreParams](GlobalData.Flow)
@data object IgnoreParams {
  def all: Set[Sym[_]] = globals[IgnoreParams].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(IgnoreParams(IgnoreParams.all + param ))
}

/** Global set of all tilesize params.
  *
  * Getter:  TileSizes.all
  * Append:  TileSizes += (mem)
  * Default: empty set
  */
case class TileSizes(params: Set[Sym[_]]) extends Data[TileSizes](GlobalData.Flow)
@data object TileSizes {
  def all: Set[Sym[_]] = globals[TileSizes].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(TileSizes(TileSizes.all + param ))
}


/** Global set of all par params.
  *
  * Getter:  ParParams.all
  * Append:  ParParams += (mem)
  * Default: empty set
  */
case class ParParams(params: Set[Sym[_]]) extends Data[ParParams](GlobalData.Flow)
@data object ParParams {
  def all: Set[Sym[_]] = globals[ParParams].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(ParParams(ParParams.all + param ))
}

/** Global set of all controllers that can be metapiped params.
  *
  * Getter:  PipelineParams.all
  * Append:  PipelineParams += (ctrl)
  * Default: empty set
  */
case class PipelineParams(params: Set[Sym[_]]) extends Data[PipelineParams](GlobalData.Flow)
@data object PipelineParams {
  def all: Set[Sym[_]] = globals[PipelineParams].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(PipelineParams(PipelineParams.all + param ))
}

/** Indentify Accel scope (top for DSE)
  *
  * Getter:  TopCtrl.all
  * Append:  TopCtrl += (r)
  * Default: empty set
  */
case class TopCtrl(r: Sym[_]) extends Data[TopCtrl](GlobalData.Flow)
@data object TopCtrl {
  def get: Sym[_] = globals[TopCtrl].map(_.r).get
  def set(r: Sym[_]): Unit = globals.add(TopCtrl(r))
}

/** Global set of all controllers that can be metapiped params.
  *
  * Getter:  Restrictions.all
  * Append:  Restrictions += (r)
  * Default: empty set
  */
case class Restrictions(r: Set[Restrict]) extends Data[Restrictions](GlobalData.Flow)
@data object Restrictions {
  def all: Set[Restrict] = globals[Restrictions].map(_.r).getOrElse(Set.empty)
  def +=(r: Restrict): Unit = globals.add(Restrictions(Restrictions.all + r ))
}


sealed abstract class Restrict {this: Product => 
  @stateful def evaluate(): Boolean
  def deps: Set[Sym[_]] = collectSet{case p: Sym[_] => p}(productIterator)
  def dependsOnlyOn(x: Sym[_]*): Boolean = (deps diff x.toSet).isEmpty
}
object Restrict {
  implicit class ParamValue(x: Sym[_]) {
    @stateful def v: Int = x.toInt
  }
}

import Restrict._

case class RLess(a: Sym[_], b: Sym[_]) extends Restrict {
  @stateful def evaluate(): Boolean = a.v < b.v
  override def toString = s"$a < $b"
}
case class RLessEqual(a: Sym[_], b: Sym[_]) extends Restrict {
  @stateful def evaluate(): Boolean = a.v <= b.v
  override def toString = s"$a <= $b"
}
case class RDivides(a: Sym[_], b: Sym[_]) extends Restrict {
  @stateful def evaluate(): Boolean = b.v % a.v == 0
  override def toString = s"$a divides $b"
}
case class RDividesConst(a: Sym[_], b: Int) extends Restrict {
  @stateful def evaluate(): Boolean = b % a.v == 0
  override def toString = s"$a divides $b"
}
case class RDividesQuotient(a: Sym[_], n: Int, d: Sym[_]) extends Restrict {
  @stateful def evaluate(): Boolean = {
    val q = Math.ceil(n.toDouble / d.v.toDouble).toInt
    a.v < q && (q % a.v == 0)
  }
  override def toString = s"$a divides ($n/$d)"
}
case class RProductLessThan(ps: Seq[Sym[_]], y: Int) extends Restrict {
  @stateful def evaluate(): Boolean = ps.map(_.v).product < y
  override def toString = s"product($ps) < $y"
}
case class REqualOrOne(ps: Seq[Sym[_]]) extends Restrict {
  @stateful def evaluate(): Boolean = {
    val values = ps.map(_.v).distinct
    values.length == 1 || (values.length == 2 && values.contains(1))
  }
  override def toString = s"$ps equal or one"
}

sealed trait SpaceType
case object Ordinal extends SpaceType { override def toString = "ordinal" }
case object Categorical extends SpaceType { override def toString = "categorical" }

case class Domain[T](name: String, options: Seq[T], setter: (T,State) => Unit, getter: State => T, tp: SpaceType) {
  def apply(i: Int): T = options(i)
  @stateful def value: T = getter(state)
  @stateful def set(i: Int): Unit = setter(options(i), state)
  @stateful def setValue(v: T): Unit = setter(v, state)
  @stateful def setValueUnsafe(v: Any): Unit = setValue(v.asInstanceOf[T])
  def len: Int = options.length

  @stateful def filter(cond: State => Boolean): Domain[T] = {
    val values = options.filter{i => setter(i, state); cond(state) }
    new Domain[T](name, values, setter, getter, tp)
  }

  override def toString: String = {
    if (len <= 10) "Domain(" + options.mkString(",") + ")"
    else "Domain(" + options.take(10).mkString(", ") + "... [" + (len-10) + " more])"
  }

  @stateful def filter(cond: => Boolean) = new Domain(name, options.filter{t => setValue(t); cond}, setter, getter, tp)
}
object Domain {
  def apply(name: String, range: Range, setter: (Int,State) => Unit, getter: State => Int, tp: SpaceType): Domain[Int] = {
    if (range.start % range.step != 0) {
      val start = range.step*(range.start/range.step + 1)
      new Domain[Int](name, (start to range.end by range.step) :+ range.start, setter, getter, tp)
    }
    else new Domain[Int](name, range, setter, getter, tp)
  }
  @stateful def restricted(name: String, range: Range, setter: (Int,State) => Unit, getter: State => Int, cond: State => Boolean, tp: SpaceType): Domain[Int] = {
    val (start, first) = if (range.start % range.step != 0) {
      val start = range.step*((range.start/range.step) + 1)
      setter(range.start, state)
      val first = if (cond(state)) Some(range.start) else None
      (start, first)
    }
    else (range.start, None)

    val values = (start to range.end by range.step).filter{i => setter(i, state); cond(state) } ++ first
    new Domain[Int](name, values, setter, getter, tp)
  }
}


