package spatial.metadata

import argon._

package object math {

  implicit class MathOps(s: Sym[_]) {
    def getModulus: Option[Int] = metadata[Modulus](s).map(_.mod)
    def modulus: Int = getModulus.getOrElse(-1)
    def modulus_=(mod: Int): Unit = metadata.add(s, Modulus(mod))
  }

}
