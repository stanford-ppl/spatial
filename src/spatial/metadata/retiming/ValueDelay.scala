package spatial.metadata.retiming

import argon._

case class ValueDelay(
    input: Sym[_],
    delay: Int,
    size:  Int,
    hierarchy: Int,
    prev: Option[ValueDelay],
    private val create: () => Sym[_])
{
  private var reg: Option[Sym[_]] = None
  def alreadyExists: Boolean = reg.isDefined
  def value(): Sym[_] = reg.getOrElse{val r = create(); reg = Some(r); r }
}

