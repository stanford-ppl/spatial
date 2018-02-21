package core

import forge.tags._
import forge.util.escapeConst
import forge.implicits.readable._

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

sealed abstract class Type[A](implicit private val ev: A <:< Top[A]) { this: Product =>
  type I
  def fresh: A
  def freshSym: Sym[A]

  final def view[B[_]<:Type[_]](value: B[A]): B[A] = value match {
    case t: Top[_] if this <:< t.tp => this.asInstanceOf[B[A]]
    case t: Top[_] => throw new Exception(s"Cannot view $this as a ${t.tp} ($this has type ${this.tp})")
    case _ => throw new Exception(r"Cannot view $this as a ${value.getClass}")
  }
  final def viewAs[B](value: B): B = value match {
    case t: Top[_] if this <:< t.tp => this.asInstanceOf[B]
    case t: Top[_] => throw new Exception(s"Cannot view $this as a ${t.tp} ($this has type ${this.tp})")
    case _ => throw new Exception(r"Cannot view $this as a ${value.getClass}")
  }
  def tp: Type[A]
  def isType: Boolean
  def viewAsSym(a: A @ uncheckedVariance): Sym[A] = ev(a)
  def viewAsTop(a: A @ uncheckedVariance): Top[A @ uncheckedVariance] = ev(a)
  def isPrimitive: Boolean
  def typeArgs: Seq[Type[_]] = Nil
  def supertypes: Seq[Type[_]] = Nil

  final def <:<(that: Type[_]): Boolean = this == that || supertypes.exists{s => s <:< that }
  final def <:>(that: Type[_]): Boolean = this <:< that && that <:< this

  final def typePrefix: String = this.productPrefix
  final def typeName: String = this.typePrefix + (if (typeArgs.isEmpty) "" else "[" + typeArgs.mkString(",") + "]")
}


// TODO: The use of @uncheckedVariance here is a little scary, double check these
// NOTE: Sym is covariant with A because Sym[B] <: Sym[A] if A <: B
// See: https://docs.scala-lang.org/tour/variances.html
sealed trait Sym[+A] extends Product with Serializable { self =>
  type I
  @inline final protected def me: A = this.asInstanceOf[A]
  private var _rhs: Def[I,A @ uncheckedVariance] = Def.TypeRef
  private var _tp: Type[A @ uncheckedVariance] = _
  private[core] val data: mutable.Map[Class[_],Metadata[_]] = mutable.Map.empty

  var name: Option[String] = None
  var ctx: SrcCtx = SrcCtx.empty
  var prevNames: Seq[(String,String)] = Nil

  final def tp: Type[A @ uncheckedVariance] = if (this.isType) this.asInstanceOf[Type[A]] else _tp
  final def rhs: Def[I, A @ uncheckedVariance] = _rhs

  final protected def asType: A = { _rhs = Def.TypeRef; me }
  final private[core] def asBound(id: Int): A = { _rhs = Def.Bound(id); me }
  final private[core] def asConst(c: Any): A = { _rhs = Def.Const(c.asInstanceOf[self.I]); me }
  final private[core] def asParam(id: Int, c: Any): A = { _rhs = Def.Param(id, c.asInstanceOf[self.I]); me }
  final private[core] def asSymbol(id: Int, op: Op[A @uncheckedVariance]): A = { _rhs = Def.Node(id, op); me }
  final private[core] def withType(t: Type[A @uncheckedVariance]): Sym[A] = { _tp = t; this }

  final def isType: Boolean = rhs.isType
  final def isConst: Boolean = rhs.isConst
  final def isParam: Boolean = rhs.isParam
  final def isValue: Boolean = rhs.isValue
  final def isBound: Boolean = rhs.isBound
  final def isSymbol: Boolean = rhs.isNode
  final def c: Option[I] = rhs.getValue
  final def op: Option[Op[A]] = rhs.getOp
  @rig final def effects: Effects = effectsOf(this)

  final def dataInputs: Seq[Sym[_]] = op.map(_.inputs).getOrElse(Nil)
}

abstract class Top[A](implicit ev: A <:< Top[A]) extends Type[A] with Sym[A] {
  type I

  final def selfTp: A = this.tp.viewAs[A](me)

  final override def freshSym: Sym[A] = {
    if (!this.isType) throw new Exception(s"Fresh call from non-Type evidence $this")
    ev(fresh).withType(this)
  }

  // TODO: rhs is a var, is this an issue?
  final override def hashCode(): Int = this.rhs match {
    case Def.Const(c)    => c.hashCode()
    case Def.Param(id,_) => id
    case Def.Node(id,_)  => id
    case Def.Bound(id)   => id
    case Def.TypeRef     => (typePrefix,typeArgs).hashCode()
  }

  final override def canEqual(x: Any): Boolean = x match {
    case _: Top[_] => true
    case _ => false
  }

  final override def equals(x: Any): Boolean = x match {
    case that: Top[_] => (this.rhs, that.rhs) match {
      case (Def.Const(a),     Def.Const(b))     => this.tp == that.tp && a == b
      case (Def.Param(idA,_), Def.Param(idB,_)) => idA == idB
      case (Def.Node(idA,_),  Def.Node(idB,_))  => idA == idB
      case (Def.Bound(idA),   Def.Bound(idB))   => idA == idB
      case (Def.TypeRef,      Def.TypeRef)      =>
        this.typePrefix == that.typePrefix && this.typeArgs == that.typeArgs
      case _ => false
    }
    case _ => false
  }

  final override def toString: String = rhs match {
    case Def.Const(c)    => s"${escapeConst(c)}"
    case Def.Param(id,c) => s"p$id (${escapeConst(c)})"
    case Def.Node(id,_)  => s"x$id"
    case Def.Bound(id)   => s"b$id"
    case Def.TypeRef     => typeName
  }
}

object Lit {
  def unapply[A<:Sym[A]](x: A): Option[A#I] = if (x.isConst) x.c else None
}
object Const {
  def unapply(x: Any): Option[Any] = x match {
    case s: Sym[_] if s.isConst => s.c
    case _ => None
  }
}
object Param {
  def unapply(x: Any): Option[Any] = x match {
    case s: Sym[_] if s.isParam || s.isConst => s.c
    case _ => None
  }
}

