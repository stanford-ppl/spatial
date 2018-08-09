package argon
package static

import forge.tags._
import utils.isSubtype
import utils.implicits.Readable._

import scala.reflect.ClassTag

class ExpTypeLowPriority[C,A](t: ExpType[C,A]) {
  def unbox: A = tp.asInstanceOf[A]
  def isType: Boolean = t match {case ref: Ref[_,_] => ref.isType }
  def tp: ExpType[C,A] = if (isType) t else t match {case ref: Ref[_,_] => ref.tp.asInstanceOf[ExpType[C,A]] }

  /** View the staged value or type as B[A]. */
  def view[B[_]](implicit tag: ClassTag[B[_]], ev: B[_] <:< ExpType[_,_]): B[A] = {
    if (isSubtype(tp.getClass,tag.runtimeClass)) tp.asInstanceOf[B[A]]
    else throw new Exception(s"Cannot view $tp (${tp.tp}) as a ${tag.runtimeClass}")
  }

  /** View the staged value or type as a B[A] if it is a subtype of B, None otherwise. */
  def getView[B[_]](implicit ev: ClassTag[B[_]]): Option[B[A]] = {
    if (isSubtype(tp.getClass,ev.runtimeClass)) Some(tp.asInstanceOf[B[A]]) else None
  }
}

class ExpTypeMiscOps[C,A](tp: ExpType[C,A]) {
  lazy val boxed: A <:< Ref[C,A] = tp.evRef

  /** Create an (optionally checked) parameter */
  @rig final def param(c: Any, checked: Boolean = false): A = tp.from(c, checked, isParam = true)

  /** Create a (optionally checked) constant */
  @rig final def const(c: Any, checked: Boolean = false): A = tp.from(c, checked)

  /** Create an unchecked constant (no implicit state required) */
  final def uconst(c: C): A = _const(tp, tp.__value(c).getOrElse(throw new Exception(r"Invalid constant type ${c.getClass} for type $tp")))

  @rig def canConvertFrom(c: Any): Boolean = tp.getFrom(c).isDefined

  /** Returns true if the type is a subtype of that type. */
  def <:<(that: ExpType[_,_]): Boolean = {
    // TODO[2]: isSubtype is a hack - need to fix this
    this =:= that || isSubtype(tp.getClass,that.getClass)
  }

  /** Returns true if the type is equivalent to that type. */
  def =:=(that: ExpType[_,_]): Boolean = {
    tp._typePrefix == that._typePrefix && tp._typeArgs == that._typeArgs &&
                                          tp._typeParams == that._typeParams
  }

  /** Returns the full name of the type. */
  def typeName: String = {
    tp._typePrefix + (if (tp._typeArgs.isEmpty) "" else "[" + tp._typeArgs.mkString(",") + "]") +
      (if (tp._typeParams.isEmpty) "" else "[" + tp._typeParams.mkString(",") + "]")
  }

  def typeArgs: Seq[Type[_]] = tp._typeArgs
  def typeParams: Seq[Any] = tp._typeParams
  def typePrefix: String = tp._typePrefix
  def neverMutable: Boolean = tp._neverMutable
}

class ExpMiscOps[C,A](exp: Exp[C,A]) {
  def unbox: A = exp.asInstanceOf[A]
  def asSym: Sym[A] = exp

  private[argon] def tp_=(tp: ExpType[C,A]): Unit = { exp._tp = tp }
  def tp: ExpType[C,A] = {
    if (exp._rhs eq null) throw new Exception(r"Val references to tp in ${exp.getClass} should be lazy")
    else if (exp.isType) exp.asInstanceOf[ExpType[C,A]]
    else if (exp._tp eq null) throw new Exception(r"Val references to tp in ${exp.getClass} should be lazy")
    else exp._tp
  }

  private[argon] def rhs_=(rhs: Def[C, A]): Unit = { exp._rhs = rhs }
  def rhs: Def[C,A] = {
    if (exp._rhs eq null) throw new Exception(r"Val references to rhs in ${exp.getClass} should be lazy")
    else exp._rhs
  }

  def name_=(str: Option[String]): Unit = { exp._name = str }
  def name: Option[String] = exp._name

  def fullname: String = name match {
    case Some(name) => s"$name ($exp)"
    case None => s"$exp"
  }
  def _name: String = name.map{n => s"_$n"}.getOrElse("")
  def nameOr(str: String): String = name.getOrElse(str).replace("$","")

  def ctx_=(ctx: SrcCtx): Unit = { exp._ctx = ctx }
  def ctx: SrcCtx = exp._ctx

  def prevNames_=(names: Seq[(String,String)]): Unit = { exp._prevNames = names }
  def prevNames: Seq[(String,String)] = exp._prevNames

  def isConst: Boolean = rhs.isConst
  def isParam: Boolean = rhs.isParam
  def isValue: Boolean = rhs.isValue
  def isBound: Boolean = rhs.isBound
  def isSymbol: Boolean = rhs.isNode
  def isType: Boolean = rhs.isType
  def c: Option[C] = rhs.getValue
  def op: Option[Op[A]] = rhs.getOp

  /** Returns data dependencies of the symbol. */
  final def inputs: Seq[Sym[_]] = op.map(_.inputs).getOrElse(Nil)
  final def nonBlockInputs: Seq[Sym[_]] = op.map(_.nonBlockExpInputs).getOrElse(Nil)
  final def blocks: Seq[Block[_]] = op.map(_.blocks).getOrElse(Nil)

  /** Returns non-data, effect dependencies of the symbol. */
  final def antiDeps: Seq[Sym[_]] = effects.antiDeps.map(_.sym)

  /** Returns all scheduling dependencies of the symbol. */
  final def allDeps: Seq[Sym[_]] = inputs ++ antiDeps

  /** Returns all symbols which uses this symbol as an input. */
  def consumers: Set[Sym[_]] = metadata[Consumers](exp).map(_.users).getOrElse(Set.empty)

  /** Sets the consumers for this symbol. */
  def consumers_=(xs: Set[Sym[_]]): Unit = metadata.add(exp, Consumers(xs))

  /** Returns the set of all nested inputs for any blocks enclosed by this symbol.
    * Lazily sets metadata for purposes of memo-ization.
    * Metadata is reset during transformations.
    **/
  def nestedInputs: Set[Sym[_]] = metadata[NestedInputs](exp).map(_.inputs).getOrElse{
    val inputs = exp.inputs.toSet ++ exp.op.map{o =>
      val outs = o.blocks.flatMap(_.nestedStms)
      val used = outs.flatMap{s => s.nestedInputs }
      used diff outs
    }.getOrElse(Set.empty)
    metadata.add(exp, NestedInputs(inputs))
    inputs
  }

  def effects: Effects = metadata[Effects](exp).getOrElse(Effects.Pure)
  def effects_=(eff: Effects): Unit = metadata.add(exp, eff)
  def isMutable: Boolean = effects.isMutable

  /** Returns all "shallow" aliases for this symbol.
    * Note that a symbol is always an alias of itself.
    **/
  def shallowAliases: Set[Sym[_]] = metadata[ShallowAliases](exp).map(_.aliases).getOrElse(Set.empty) + exp
  def shallowAliases_=(aliases: Set[Sym[_]]): Unit = metadata.add(exp, ShallowAliases(aliases))

  /** Returns all "deep" aliases for this symbol.
    * Note that a symbol is always an alias of itself.
    */
  def deepAliases: Set[Sym[_]] = metadata[DeepAliases](exp).map(_.aliases).getOrElse(Set.empty) + exp
  def deepAliases_=(aliases: Set[Sym[_]]): Unit = metadata.add(exp, DeepAliases(aliases))

  def allAliases: Set[Sym[_]] = shallowAliases ++ deepAliases
  def mutableAliases: Set[Sym[_]] = allAliases.filter(_.isMutable)

  /** View the staged value or type as B[A]. */
  def view[B[_]](implicit tag: ClassTag[B[_]], ev: B[_] <:< ExpType[_,_]): B[A] = {
    if (isSubtype(exp.getClass,tag.runtimeClass)) exp.asInstanceOf[B[A]]
    else throw new Exception(s"Cannot view $exp ($tp) as a ${tag.runtimeClass}")
  }

  /** View the staged value or type as a B[A] if it is a subtype of B, None otherwise. */
  def getView[B[_]](implicit ev: ClassTag[B[_]]): Option[B[A]] = {
    if (isSubtype(exp.getClass,ev.runtimeClass)) Some(exp.asInstanceOf[B[A]]) else None
  }
}

/** Defines implicit infix methods available on staged types and symbols
  *
  * These are implicits rather than defined in the classes themselves to avoid
  * having them polluting the application namespace by default.
  */
trait LowPriorityImplicits { this: Implicits =>
  implicit def lowPriorityExpType[C,A](tp: ExpType[C,A]): ExpTypeLowPriority[C,A] = new ExpTypeLowPriority[C,A](tp)
}

trait Implicits extends LowPriorityImplicits {
  implicit def expTypeOps[C,A](tp: ExpType[C,A]): ExpTypeMiscOps[C,A] = new ExpTypeMiscOps[C,A](tp)
  implicit def expOps[C,A](exp: Exp[C,A]): ExpMiscOps[C,A] = new ExpMiscOps[C,A](exp)
}
