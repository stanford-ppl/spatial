package spatial.util

import forge.tags._
import spatial.libdsl._
import spatial.metadata.memory._

object debug {
  @api def tagValue[T: Bits](v: T, name: String): Unit = {
    val reg = Reg[T](implicitly[Bits[T]].zero)
    reg.explicitName = name
    reg.dontTouch
    reg := v
  }
}
