package argon

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
abstract class ExpType[+C:ClassTag,A](implicit protected[argon] val evRef: A <:< Ref[C,A]) extends Equals {
  type L = C@uV

  protected def me: A = this.asInstanceOf[A]


  /** True if an instance of this type can never be mutable. */
  protected def __neverMutable: Boolean

  /** Returns the name of this type */
  protected def __typePrefix: String = r"${this.getClass}"

  /** Returns a list of the type arguments of this staged type. */
  protected def __typeArgs: Seq[Type[_]] = throw new Exception(s"Override typeArgs or use the @ref annotation in ${__typePrefix}")
  protected def __typeParams: Seq[Any] = Nil

  final private[argon] def _typePrefix: String = __typePrefix
  final private[argon] def _neverMutable: Boolean = __neverMutable
  final private[argon] def _typeArgs: Seq[Type[_]] = __typeArgs
  final private[argon] def _typeParams: Seq[Any] = __typeParams

  /** Returns a new (raw) staged value of type A. */
  protected def fresh: A = throw new Exception(s"Override fresh or use @ref annotation in ${__typePrefix}")
  final private[argon] def _new(d: Def[C@uV,A@uV], ctx: SrcCtx): A = {
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
    case _ => None
  }
  final private[argon] def __value(c: Any): Option[C] = value(c).map(_._1)

  /** Create a checked value from the given constant
    * Value may be either a constant or a parameter
    */
  @rig final def from(c: Any, warnOnLoss: Boolean = false, errorOnLoss: Boolean = false, isParam: Boolean = false, saturating: Boolean = false, unbiased: Boolean = false): A = getFrom(c,isParam,saturating,unbiased) match {
    case Some((v,exact)) =>
      if (!exact && errorOnLoss) {
        error(ctx, s"Loss of precision detected: ${this.tp} cannot exactly represent ${escapeConst(c)}.")
        error(s"(Closest representable value: ${escapeConst(evRef(v).c.get)}).")
        error(s"""Use .to[${this.tp}] to downgrade to a warning, or .toUnchecked to ignore.""")
        error(ctx)
      }
      else if (!exact && warnOnLoss) {
        warn(ctx, s"Loss of precision detected: ${this.tp} cannot exactly represent ${escapeConst(c)}.")
        warn(s"(Closest representable value: ${escapeConst(evRef(v).c.get)})")
        warn(s"""Use .toUnchecked to ignore this warning.""")
        warn(ctx)
      }
      v
    case None =>
      implicit val tA: Type[A] = this
      error(ctx, r"Cannot convert ${escapeConst(c)} with type ${c.getClass} to a ${this.tp}")
      error(ctx)
      err[A]("Invalid constant")
  }

  /** Attempt to create a symbol of type A from c, where the type of c is unrestricted.
    * This method should never throw exceptions or produce warnings or errors.
    * Instead, it should return None if c is not convertible to an A, or return
    * Some((a,false)) if c can be represented as an A, but not exactly.
    */
  @rig def getFrom(c: Any, isParam: Boolean = false, saturating: Boolean = false, unbiased: Boolean = false): Option[(A,Boolean)] = value(c) match {
    case Some((v,exact)) if isParam => Some((_param(this, v), exact))
    case Some((v,exact)) => Some((_const(this, v),exact))
    case None => None
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
sealed trait Exp[+C,+A] extends Equals { self =>
  type L = C@uV
  type R = A@uV

  private[argon] var _tp: ExpType[C@uV,A@uV] = _
  private[argon] var _rhs: Def[C@uV,A@uV] = _
  private[argon] val _data: mutable.Map[Class[_],Data[_]] = mutable.Map.empty

  private[argon] var _name: Option[String] = None
  private[argon] var _ctx: SrcCtx = SrcCtx.empty
  private[argon] var _prevNames: Seq[(String,String)] = Nil

  /** Extract the constant value of the symbol as a Scala primitive
    * This is a hack to get around the fact it is impossible to make new classes
    * with value equality with Int, Long, Float, Double, etc.
    */
  protected def extract: Option[Any] = this.c
  private[argon] def __extract: Option[Any] = extract
}


/**
  * Either a staged expression or a staged type.
  *
  * All staged types should mix in this trait last.
  */
trait Ref[+C,+A] extends ExpType[C,A@uV] with Exp[C,A]  {
  override type L = C@uV

  final override def hashCode(): Int = this.rhs match {
    case Def.Const(c)    => c.hashCode()
    case Def.Param(id,_) => id
    case Def.Node(id,_)  => id
    case Def.Bound(id)   => id
    case Def.Error(id,_) => id
    case Def.TypeRef     => (_typePrefix,_typeArgs).hashCode()
  }

  final override def canEqual(that: Any): Boolean = that.isInstanceOf[Ref[_,_]]

  final override def equals(x: Any): Boolean = x match {
    case that: Ref[_,_] => (this.rhs, that.rhs) match {
      case (Def.Const(a),     Def.Const(b))     => this.tp =:= that.tp && a == b
      case (Def.Param(idA,_), Def.Param(idB,_)) => idA == idB
      case (Def.Node(idA,_),  Def.Node(idB,_))  => idA == idB
      case (Def.Bound(idA),   Def.Bound(idB))   => idA == idB
      case (Def.Error(idA,_), Def.Error(idB,_)) => idA == idB
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
    case Def.Error(_,_)  => s"<error>"
    case Def.TypeRef     => expTypeOps(this).typeName
  }
}

