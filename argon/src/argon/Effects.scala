package argon

/** The effects summary of a symbol.
  *
  * Effects metadata is "Stable" since it should never be removed, but we always take the "old"
  * metadata during merging since this is the one created by staging.
  *
  * @param unique Should not be CSEd
  * @param sticky Should not be code motioned out of blocks
  * @param simple Requires ordering with respect to other simple effects
  * @param global Modifies execution of the entire program (e.g. exceptions, exiting)
  * @param mutable Allocates a mutable structure
  * @param throws May throw exceptions (so speculative execution is unsafe)
  * @param reads A set of read mutable symbols
  * @param writes A set of written mutable symbols
  * @param antiDeps Anti-dependencies of this operation (must come before this symbol)
  */
case class Effects(
  unique:  Boolean = false,
  sticky:  Boolean = false,
  simple:  Boolean = false,
  global:  Boolean = false,
  mutable: Boolean = false,
  throws:  Boolean = false,
  reads:   Set[Sym[_]] = Set.empty,
  writes:  Set[Sym[_]] = Set.empty,
  antiDeps: Seq[Impure] = Nil
) extends StableData[Effects] {

  /** Always prefer the old effects metadata since this is updated during staging. */
  override def merge(old: Effects): Effects = old

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

  def isPure: Boolean = this == Effects.Pure
  def isMutable: Boolean = mutable
  def isIdempotent: Boolean = !simple && !global && !mutable && writes.isEmpty && !throws
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

case class Impure(sym: Sym[_], effects: Effects)
object Impure {
  def unapply(x: Sym[_]): Option[(Sym[_],Effects)] = {
    val effects = x.effects
    if (effects.isPure && effects.antiDeps.isEmpty) None else Some((x,effects))
  }
}
