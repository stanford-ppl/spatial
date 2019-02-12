package spatial.lang.api

import argon._
import forge.tags._
import spatial.node._

trait ShuffleAPI { this: Implicits =>

  @rig def compress[A:Bits](in: Tup2[A,Bit]): Tup2[A,Bit] = {
    stage(ShuffleCompress(in))
  }

}
