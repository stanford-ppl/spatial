package pcc.lang
package control

import forge._
import pcc.core._
import pcc.node._

object Pipe {
  @api def apply(block: => Void): Void = Pipe.apply()(block)
  @api def apply(ens: Bit*)(block: => Void): Void = stage(UnitPipe(ens,stageBlock{ block }))
}


