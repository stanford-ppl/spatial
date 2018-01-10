package pcc
package ir
package control

import forge._

object Accel {
  @api def apply(x: => Void): Void = accel(() => x)
  @api def accel(x: () => Void): Void = {
    val blk = stageBlock(x())
    stage(Accel(blk))
  }
}


case class Accel(block: Block[Void]) extends Pipeline {
  def ens: Seq[Bit] = Nil
  def mirror(f:Tx) = Accel.accel(f.tx(block))
}