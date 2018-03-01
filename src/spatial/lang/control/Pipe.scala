package spatial.lang
package control

import forge.tags._
import core._
import spatial.node._

object Pipe {
  @api def apply(block: => Void): Void = Pipe.apply()(block)
  @api def apply(ens: Bit*)(block: => Void): Void = stage(UnitPipe(ens, stageBlock{ block }))
}
