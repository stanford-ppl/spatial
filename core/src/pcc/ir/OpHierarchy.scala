package pcc
package ir

/** Memories **/
abstract class Alloc[T:Sym] extends Op[T]

/** Control **/
abstract class Control extends Op[Void]

abstract class EnabledControl extends Control {
  def ens: Seq[Bit]
}

abstract class Pipeline extends EnabledControl
abstract class Loop extends Pipeline

/** Primitives **/
abstract class Stateless[A:Sym] extends Op[A]

abstract class Primitive[A:Sym] extends Op[A]
abstract class EnabledPrimitive[A:Sym] extends Primitive[A] {
  def ens: Seq[Bit]
}

/** Memory accesses **/
abstract class Access {
  def mem:  Sym[_]
  def addr: Option[Seq[I32]]
  def ens:  Seq[Bit]
}
case class Read(mem: Sym[_], addr: Option[Seq[I32]], ens: Seq[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Option[Seq[I32]], ens: Seq[Bit]) extends Access


abstract class Accessor[A:Sym,R:Sym] extends EnabledPrimitive[R] {
  val tA: Sym[A] = tp[A]
  def localReads: Seq[Read]
  def localWrites: Seq[Write]
}

abstract class Reader[A:Sym,R:Sym](
  mem: Sym[_],
  adr: Option[Seq[I32]],
  ens: Seq[Bit]
) extends Accessor[A,R] {
  def localReads = Seq(Read(mem,adr,ens))
  def localWrites = Nil
}

object Reader {
  def unapply(x: Sym[_]): Option[Seq[Read]] = x match {
    case Op(a: Accessor[_,_]) if a.localReads.nonEmpty => Some(a.localReads)
    case _ => None
  }
}

abstract class Writer[A:Sym](
  mem: Sym[_],
  dat: Sym[_],
  adr: Option[Seq[I32]],
  en:  Seq[Bit]
) extends Accessor[A,Void] {
  def localReads = Nil
  def localWrites = Seq(Write(mem,dat,adr,ens))
}

object Writer {
  def unapply(x: Sym[_]): Option[Seq[Write]] = x match {
    case Op(a: Accessor[_,_]) if a.localWrites.nonEmpty => Some(a.localWrites)
    case _ => None
  }
}