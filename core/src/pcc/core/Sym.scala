package pcc
package core

import pcc.util.Tri._
import pcc.util.Types._
import pcc.util.escapeConst

abstract class Sym[A](eid: Int)(implicit ev: A<:<Sym[A]) extends Product { self =>
  type I
  private def me: A = this.asInstanceOf[A]
  private var _rhs: Tri[I,Op[A]] = Nix
  private var isFixed: Boolean = true

  var name: Option[String] = None
  var ctx: SrcCtx = SrcCtx.empty
  var prevNames: Seq[(String,String)] = Nil

  def fixed(): Unit = { isFixed = true }
  def asBound(): A = { _rhs = Nix; me }
  def asConst(c: Any): A = { _rhs = One(c.asInstanceOf[self.I]); me }
  def asParam(c: Any): A = { _rhs = One(c.asInstanceOf[self.I]); isFixed = false; me }
  def asSymbol(rhs: Op[A]): A = { _rhs = Two(rhs); me }
  def isConst: Boolean = { _rhs.isOne && isFixed }
  def isParam: Boolean = { _rhs.isOne && isFixed }
  def isBound: Boolean = { id >= 0 && _rhs.isNix }
  def isSymbol: Boolean = { _rhs.isTwo }
  def isType: Boolean = { id < 0 }
  def rhs: Tri[I,Op[A]] = _rhs
  def c: Option[I] = _rhs.getOne
  def op: Option[Op[A]] = _rhs.getTwo
  def id: Int = eid

  def asSym(x: A): Sym[A] = ev(x)
  def asSym: Sym[A] = this

  override def toString: String = if (isType) this.productPrefix else _rhs match {
      case One(c) => s"${escapeConst(c)}"
      case Two(_) => s"b$id"
      case Nix    => s"b$id"
  }

  def fresh(id: Int): A
  def isPrimitive: Boolean
  def typeArguments: List[Sym[_]] = Nil
  def stagedClass: Class[A]

  final def <:<(that: Sym[_]): Boolean = isSubtype(this.stagedClass, that.stagedClass)
  final def <:>(that: Sym[_]): Boolean = this <:< that && that <:< this
}

object Lit {
  def unapply[A<:Sym[A]](x: A): Option[A#I] = if (x.isConst) x.c else None
}

