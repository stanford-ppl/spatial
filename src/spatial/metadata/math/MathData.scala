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

/** Flag marking whether or not this node is part of an accumulation cycle, in order to
  * avoid rewriting multiple nodes where some are part of a cycle and some are not (i.e. FMA rewrites) 
  *  
  *
  * Getter:  sym.inCycle = Boolean
  * Setter:  sym.inCycle = is
  * Default: false
  */
case class InCycle(is: Boolean) extends Data[InCycle](SetBy.Analysis.Self) 
