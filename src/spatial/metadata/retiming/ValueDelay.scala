package spatial.metadata.retiming

import argon._

case class ValueDelay(
    input: Sym[_],
    delay: Int,
    size:  Int,
    hierarchy: Int,
    prev: Option[ValueDelay],
    private val create: Option[() => Sym[_]])
{
  private var reg: Option[Sym[_]] = None
  def alreadyExists: Boolean = reg.isDefined
  def value(): Sym[_] = reg.getOrElse{ create match {
    case Some(line) => val r = line(); reg = Some(r); r
    case None => throw new Exception(s"Cannot create delay line - no creation method given.")
  }}
}

