package spatial.metadata.math

import argon._
import emul.ResidualGenerator._

/** Flag marking the modulus of a node, only defined on FixMod and nodes that FixMod lowers to
  *
  * Getter:  sym.getModulus = Option[Int]
  * Setter:  sym.modulus = mod
  * Default: None
  */
case class Modulus(mod: Int) extends Data[Modulus](SetBy.Analysis.Self)

/** Flag marking the equation to generate residual set for modulo. The residual set of this
  * equation is the numbers that can be congruent to (A*k + B) mod M
  *  
  *
  * Getter:  sym.getResidual = Option[Int]
  * Setter:  sym.residual = mod
  * Default: None
  */
case class Residual(equ: ResidualGenerator) extends Data[Residual](SetBy.Analysis.Self) 
