package argon.lang.api

import argon._
import forge.tags._

trait BitsAPI {

  @api def zero[A:Bits]: A = Bits[A].zero
  @api def one[A:Bits]: A = Bits[A].one

  @api def random[A:Bits]: A = Bits[A].random(None)
  @api def random[A:Bits](max: A): A = Bits[A].random(Some(max))

}
