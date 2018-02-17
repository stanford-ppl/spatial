package nova.lang
package control

import forge.tags._
import nova.core._
import nova.node._

object Pipe {
  @api def apply(block: => Void): Void = Pipe.apply()(block)
  @api def apply(ens: Bit*)(block: => Void): Void = stage(UnitPipe(ens,stageBlock{ block }))
}
