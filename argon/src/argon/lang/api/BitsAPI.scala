package argon.lang.api

import argon._
import forge.tags._

trait BitsAPI {

  @api def zero[A:Bits]: A = Bits[A].zero
  @api def one[A:Bits]: A = Bits[A].one

  @api def random[A:Bits]: A = Bits[A].random(None)
  @api def random[A:Bits](max: A): A = Bits[A].random(Some(max))

  @api def cat(c: Vec[Bit]*): Vec[Bit] = Vec.concat(c.reverse)
  @api def catSeq(c: Seq[Bit]) : Vec[Bit] = Vec.concat(Seq.tabulate(c.length){ i => c(i).asBits })
  
  @api def popcount(c : Seq[Bit]) : U8 = Vec.popcount(c)

  
//  @api def popcount[A:Bits](c : Bits, size : Int) : U8 = Vec.popcount(Seq.tabulate(size) {i => c.asBits.apply(i) })
}
