package core

import forge.tags._
import utils.implicits.Readable._
import utils.{escapeConst, isSubtype}

import scala.collection.mutable
import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.reflect.{ClassTag,classTag}

/** Staged type evidence.
  *
  * ExpType[C,A] provides evidence that A is a staged value with constant type C.
  *
  * Note that ExpType is NOT covariant with A
  */
abstract class ExpType[+C:ClassTag,A](implicit protected[core] val evRef: A <:< Ref[C,A]) extends Serializable with Equals {
  type L = C@uV

  protected def me: A = this.asInstanceOf[A]


  /** True if this type is a primitive */
  protected def __isPrimitive: Boolean

  /** Returns the name of this type */
  protected def __typePrefix: String = r"${this.getClass}"

  /** Returns a list of the type arguments of this staged type. */
  protected def __typeArgs: Seq[Type[_]] = throw new Exception(s"Override typeArgs or use the @ref annotation in ${__typePrefix}")
  protected def __typeParams: Seq[Any] = Nil

  final private[core] def _typePrefix: String = __typePrefix
  final private[core] def _isPrimitive: Boolean = __isPrimitive
  final private[core] def _typeArgs: Seq[Type[_]] = __typeArgs
  final private[core] def _typeParams: Seq[Any] = __typeParams

  /** Returns a new (raw) staged value of type A. */
  protected def fresh: A = throw new Exception(s"Override fresh or use @ref annotation in ${__typePrefix}")
  final private[core] def _new(d: Def[C@uV,A@uV], ctx: SrcCtx): A = {
    val v = fresh
    evRef(v).tp = this.tp
    evRef(v).rhs = d
    evRef(v).ctx = ctx
    v
  }

  /** Returns a value along with a flag for whether the conversion is exact. */
  final protected def withCheck[T](x: => T)(eql: T => Boolean): Option[(T,Boolean)] = {
    try {
      val v = x
      Some((v,eql(v)))
    }
    catch {case _: Throwable => None }
  }

  protected def value(c: Any): Option[(C,Boolean)] = c match {
    case x: java.lang.Character => this.value(x.toChar)
    case x: java.lang.Byte    => this.value(x.toByte)
    case x: java.lang.Short   => this.value(x.toShort)
    case x: java.lang.Integer => this.value(x.toInt)
    case x: java.lang.Long    => this.value(x.toLong)
    case x: java.lang.Float   => this.value(x.toFloat)
    case x: java.lang.Double  => this.value(x.toDouble)
    case _ if isSubtype(c.getClass,classTag[C].runtimeClass) => Some((c.asInstanceOf[C],true))
    case _ => None
  }
  final private[core] def __value(c: Any): Option[C] = value(c).map(_._1)

  /** Create a checked value from the given constant
    * Value may be either a constant or a parameter
    */
  @rig final def from(c: Any, checked: Boolean = false, isParam: Boolean = false): A = value(c) match {
    case Some((v,exact)) =>
      if (!exact && checked) {
        error(ctx, s"Loss of precision detected: ${this.tp} cannot exactly represent value ${escapeConst(c)}.")
        error(s"""Use the explicit annotation "${escapeConst(c)}.to[${this.tp}]" to ignore this error.""")
        error(ctx)
      }

      if (isParam) _param(this, v)
      else         _const(this, v)

    case None =>
      implicit val tA: Type[A] = this
      error(ctx, r"Cannot convert ${escapeConst(c)} with type ${c.getClass} to a ${this.tp}")
      error(ctx)
      err[A]
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

  private[core] var _tp: ExpType[C@uV,A@uV] = _
  private[core] var _rhs: Def[C@uV,A@uV] = _
  private[core] val data: mutable.Map[Class[_],Metadata[_]] = mutable.Map.empty

  private[core] var _name: Option[String] = None
  private[core] var _ctx: SrcCtx = SrcCtx.empty
  private[core] var _prevNames: Seq[(String,String)] = Nil

  /** Extract the constant value of the symbol as a Scala primitive
    * This is a hack to get around the fact it is impossible to make new classes
    * with value equality with Int, Long, Float, Double, etc.
    */
  protected def extract: Option[Any] = this.c
  private[core] def __extract: Option[Any] = extract
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
    case Def.TypeRef     => (_typePrefix,_typeArgs).hashCode()
  }

  final override def canEqual(that: Any): Boolean = that.isInstanceOf[Ref[_,_]]

  final override def equals(x: Any): Boolean = x match {
    case that: Ref[_,_] => (this.rhs, that.rhs) match {
      case (Def.Const(a),     Def.Const(b))     => this.tp =:= that.tp && a == b
      case (Def.Param(idA,_), Def.Param(idB,_)) => idA == idB
      case (Def.Node(idA,_),  Def.Node(idB,_))  => idA == idB
      case (Def.Bound(idA),   Def.Bound(idB))   => idA == idB
      case (Def.Error(idA),   Def.Error(idB))   => idA == idB
      case (Def.TypeRef,      Def.TypeRef)      => this =:= that
      case _ => false
    }
    case _ => false
  }

  final override def toString: String = this.rhs match {
    case Def.Const(c)    => s"Const(${escapeConst(c)})"
    case Def.Param(id,c) => s"p$id (${escapeConst(c)})"
    case Def.Node(id,_)  => s"x$id"
    case Def.Bound(id)   => s"b$id"
    case Def.Error(_)    => s"<error>"
    case Def.TypeRef     => expTypeOps(this).typeName
  }
}

