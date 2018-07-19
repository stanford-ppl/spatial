package spatial.metadata.math

import argon._

/** Flag marking the modulus of a node, only defined on FixMod and nodes that FixMod lowers to
  *
  * Getter:  sym.getModulus = Option[Int]
  * Setter:  sym.modulus = mod
  * Default: None
  */
case class Modulus(mod: Int) extends Data[Modulus](SetBy.Analysis.Self)
