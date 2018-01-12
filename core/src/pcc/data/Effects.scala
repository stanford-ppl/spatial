package pcc.data

import forge._
import pcc.core._

case class AntiDeps(syms: Seq[Sym[_]]) extends ComplexData[AntiDeps]

case class Effects(
  unique:  Boolean = false,           // Should not be CSEd
  sticky:  Boolean = false,           // Should not be code motioned out of blocks
  simple:  Boolean = false,           // Requires ordering with respect to other simple effects
  global:  Boolean = false,           // Modifies execution of entire program (e.g. exceptions, exiting)
  mutable: Boolean = false,           // Allocates a mutable structure
  throws:  Boolean = false,           // May throw exceptions (speculative execution may be unsafe)
  reads:   Set[Sym[_]] = Set.empty,   // Reads given mutable symbols
  writes:  Set[Sym[_]] = Set.empty    // Writes given mutable symbols
) extends ComplexData[Effects] {

  private def combine(that: Effects, m1: Boolean, m2: Boolean) = Effects(
    unique  = this.unique || that.unique,
    sticky  = this.sticky || that.sticky,
    simple  = this.simple || that.simple,
    global  = this.global || that.global,
    mutable = (m1 && this.mutable) || (m2 && that.mutable),
    throws  = this.throws || that.throws,
    reads   = this.reads union that.reads,
    writes  = this.writes union that.writes
  )
  def orElse(that: Effects): Effects = this.combine(that, m1 = false, m2 = false)
  def andAlso(that: Effects): Effects = this.combine(that, m1 = true, m2 = true)
  def andThen(that: Effects): Effects = this.combine(that, m1 = false, m2 = true)
  def star: Effects = this.copy(mutable = false) // Pure orElse this

  def isPure: Boolean = this == Effects.Pure || this == Effects.Sticky
  def isMutable: Boolean = mutable
  def isIdempotent: Boolean = !simple && !global && !mutable && writes.isEmpty
  def mayCSE: Boolean = isIdempotent && !unique

  def mayWrite(ss: Set[Sym[_]]): Boolean = global || ss.exists { s => writes contains s }
  def mayRead(ss: Set[Sym[_]]): Boolean = global || ss.exists { s => reads contains s }

  override def toString: String = {
    if      (this == Effects.Pure)    "Pure"
    else if (this == Effects.Unique)  "Unique"
    else if (this == Effects.Sticky)  "Sticky"
    else if (this == Effects.Mutable) "Mutable"
    else if (this == Effects.Simple)  "Simple"
    else if (this == Effects.Global)  "Global"
    else if (this == Effects.Throws)  "Throws"
    else {
      "(" +
        ((if (this.unique) List(s"unique=${this.unique}") else Nil) ++
          (if (this.sticky) List(s"sticky=${this.sticky}") else Nil) ++
          (if (this.simple) List(s"simple=${this.simple}") else Nil) ++
          (if (this.global) List(s"global=${this.global}") else Nil) ++
          (if (this.mutable)  List("mutable") else Nil) ++
          (if (this.throws) List("throws") else Nil) ++
          (if (this.reads.nonEmpty) List(s"""reads={${this.reads.map(x=> s"$x").mkString(",")}}""") else Nil) ++
          (if (this.writes.nonEmpty) List(s"""writes={${this.writes.map(x=> s"$x").mkString(",")}}""") else Nil)).mkString(", ") + ")"
    }
  }
}

object Effects {
  lazy val Pure = Effects()
  lazy val Sticky = Effects(sticky = true)
  lazy val Unique = Effects(unique = true)
  lazy val Simple = Effects(simple = true)
  lazy val Global = Effects(global = true)
  lazy val Mutable = Effects(mutable = true)
  lazy val Throws = Effects(throws = true)

  def Writes(x: Sym[_]*) = Effects(writes = x.toSet)
  def Reads(x: Set[Sym[_]]) = Effects(reads = x)
}



object Effectful {
  @stateful def unapply(x: Sym[_]): Option[(Effects,Seq[Sym[_]])] = {
    val deps = depsOf(x)
    val effects = effectsOf(x)
    if (effects.isPure && deps.isEmpty) None else Some((effects,deps))
  }
}

object depsOf {
  @stateful def apply(x: Sym[_]): Seq[Sym[_]] = metadata[AntiDeps](x).map(_.syms).getOrElse(Nil)
  @stateful def update(x: Sym[_], deps: Seq[Sym[_]]): Unit = metadata.add(x, AntiDeps(deps))
}

object effectsOf {
  @stateful def apply(s: Sym[_]): Effects = metadata[Effects](s).getOrElse(Effects.Pure)
  @stateful def update(s: Sym[_], e: Effects): Unit = metadata.add(s, e)
}

object isMutable {
  @stateful def apply(s: Sym[_]): Boolean = effectsOf(s).isMutable

}



