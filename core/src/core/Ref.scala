package core

import forge.tags._
import utils.implicits.Readable._
import utils.{escapeConst,isSubtype}

import scala.collection.mutable
import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.reflect.ClassTag

/** Staged type evidence.
  *
  * ExpType[C,A] provides evidence that A is a staged value with constant type C.
  *
  * Note that ExpType is NOT covariant with A
  */
abstract class ExpType[+C,A](implicit ev: A <:< Ref[C,A]) extends Serializable with Equals {
  type L = C@uV
  def tp: ExpType[C,A]
  val boxed: A <:< Ref[C,A] = ev

  protected def me: A = this.unbox
  def unbox: A = this.asInstanceOf[A]

  /** Returns the name of this type **/
  def typePrefix: String = r"${this.getClass}"

  /** True if this type is a primitive **/
  def isPrimitive: Boolean

  /** Returns a list of the type arguments of this staged type. **/
  def typeArgs: Seq[Type[_]] = throw new Exception(s"Override typeArgs or use the @ref annotation in $typePrefix")

  /** Returns a new (raw) staged value of type A. **/
  protected def fresh: A = throw new Exception(s"Override fresh or use @ref annotation in $typePrefix")
  final private[core] def _new(d: Def[C@uV,A@uV]): A = {val v = fresh; v.tp = this; v.rhs = d; v }

  /** View this staged value or type as B[A]. **/
  final def view[B[_]](implicit tag: ClassTag[B[_]], ev: B[_] <:< ExpType[_,_]): B[A@uV] = {
    if (isSubtype(this.getClass,tag.runtimeClass)) this.asInstanceOf[B[A]]
    else throw new Exception(s"Cannot view $this (${this.tp}) as a ${tag.runtimeClass}")
  }

  /** View this staged value or type as a B[A] if it is a subtype of B, None otherwise. **/
  final def getView[B[_]](implicit ev: ClassTag[B[_]]): Option[B[A@uV]] = {
    if (isSubtype(this.getClass,ev.runtimeClass)) Some(this.asInstanceOf[B[A]]) else None
  }

  /**
    * Returns true if this type is a subtype of that type.
    */
  final def <:<(that: ExpType[_,_]): Boolean = this =:= that || isSubtype(this.getClass,that.getClass)

  /**
    * Returns true if this type is equivalent to that type.
    */
  final def =:=(that: ExpType[_,_]): Boolean = {
    this.typePrefix == that.typePrefix && this.typeArgs == that.typeArgs
  }

  /**
    * Returns the full name of this type.
    */
  final def typeName: String = {
    typePrefix + (if (typeArgs.isEmpty) "" else "[" + typeArgs.mkString(",") + "]")
  }

  @rig def withCheck[T](x: T, checked: Boolean)(eql: T => Boolean): Option[T] = {
    if (checked && !eql(x)) {
      error(ctx, s"Loss of precision detected: ${this.tp} cannot exactly represent value ${escapeConst(x)}.")
      error(s"""Use the explicit annotation "${escapeConst(x)}.to[${this.tp}]" to ignore this error.""")
      error(ctx)
    }
    Some(x)
  }

  @rig def cnst(c: Any, checked: Boolean = true): Option[C] = None

  @rig final def from(c: Any, checked: Boolean = true, isParam: Boolean = false): A = {
    implicit val tA: Type[A] = this
    (cnst(c,checked),isParam) match {
      case (Some(x),true)  => _param(this, x)
      case (Some(x),false) => _const(this, x)
      case (None,_) =>
        error(ctx, s"Cannot convert ${c.getClass} to a ${this.tp}")
        error(ctx)
        err[A]
    }
  }
  final def uconst(c: C@uV): A = {
    implicit val tA: Type[A] = this
    _const(this, c)
  }
}



/** A staged expression.
  *
  * An Exp[C,S] may represent:
  *   1. A bound value of type S (no defining node)
  *   2. A constant value of type C (immutable literal value, no defining node)
  *   3. A parameter of type C (mutable literal value, no defining node)
  *   4. The result of an operation of type S (has a defining node)
  */
sealed trait Exp[+C,+A] extends Serializable with Equals { self =>
  type L = C@uV
  def unbox: A

  private[core] var _tp: ExpType[C@uV,A@uV] = _
  private[core] def tp_=(tp: ExpType[C@uV,A@uV]): Unit = { _tp = tp }
  final def tp: ExpType[C,A@uV] = {
    if (_rhs == null) throw new Exception(r"Val references to tp in ${this.getClass} should be lazy")
    else if (this.isType) this.asInstanceOf[ExpType[C,A]]
    else if (_tp == null) throw new Exception(r"Val references to tp in ${this.getClass} should be lazy")
    else _tp
  }

  //final def selfType: A = tp.asInstanceOf[A]

  private var _rhs: Def[C@uV,A@uV] = _
  private[core] def rhs_=(rhs: Def[C@uV, A@uV]): Unit = { _rhs = rhs }
  final def rhs: Def[C,A] = {
    if (_rhs == null) throw new Exception(r"Val references to rhs in ${this.getClass} should be lazy")
    else _rhs
  }

  final def asType: A = { _rhs = Def.TypeRef; this.asInstanceOf[A] }

  private[core] val data: mutable.Map[Class[_],Metadata[_]] = mutable.Map.empty

  var name: Option[String] = None
  var src: SrcCtx = SrcCtx.empty
  var prevNames: Seq[(String,String)] = Nil

  final def isConst: Boolean = rhs.isConst
  final def isParam: Boolean = rhs.isParam
  final def isValue: Boolean = rhs.isValue
  final def isBound: Boolean = rhs.isBound
  final def isSymbol: Boolean = rhs.isNode
  final def isType: Boolean = rhs.isType
  final def c: Option[C] = rhs.getValue
  final def op: Option[Op[A@uV]] = rhs.getOp

  /** Returns data dependencies of this symbol. **/
  final def inputs: Seq[Sym[_]] = op.map(_.inputs).getOrElse(Nil)

  /** Returns non-data, effect dependencies of this symbol. **/
  @stateful final def antiDeps: Seq[Sym[_]] = effectsOf(this).antiDeps.map(_.sym)

  /** Returns all scheduling dependencies of this symbol. **/
  @stateful final def allDeps: Seq[Sym[_]] = inputs ++ antiDeps
}


/**
  * Either a staged expression or a staged type.
  *
  * All staged types should mix in this trait last.
  */
trait Ref[+C,+A] extends ExpType[C,A@uV] with Exp[C,A] {
  override type L = C@uV

  final override def hashCode(): Int = this.rhs match {
    case Def.Const(c)    => c.hashCode()
    case Def.Param(id,_) => id
    case Def.Node(id,_)  => id
    case Def.Bound(id)   => id
    case Def.Error(id)   => id
    case Def.TypeRef     => (typePrefix,typeArgs).hashCode()
  }

  final override def canEqual(that: Any): Boolean = that.isInstanceOf[Ref[_,_]]

  final override def equals(x: Any): Boolean = x match {
    case that: Ref[_,_] => (this.rhs, that.rhs) match {
      case (Def.Const(a),     Def.Const(b))     => this.tp == that.tp && a == b
      case (Def.Param(idA,_), Def.Param(idB,_)) => idA == idB
      case (Def.Node(idA,_),  Def.Node(idB,_))  => idA == idB
      case (Def.Bound(idA),   Def.Bound(idB))   => idA == idB
      case (Def.Error(idA),   Def.Error(idB))   => idA == idB
      case (Def.TypeRef,      Def.TypeRef)      => this =:= that
      case _ => false
    }
    case _ => false
  }

  final override def toString: String = rhs match {
    case Def.Const(c)    => s"${escapeConst(c)}"
    case Def.Param(id,c) => s"p$id (${escapeConst(c)})"
    case Def.Node(id,_)  => s"x$id"
    case Def.Bound(id)   => s"b$id"
    case Def.Error(_)    => s"<error>"
    case Def.TypeRef     => typeName
  }
}

