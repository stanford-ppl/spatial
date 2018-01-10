package pcc
package ir
package control

object UnitPipe {
  def staged(ens: Seq[Bit], block: () => Void): Void = {
    stage(UnitPipe(ens,stageBlock{block()}))
  }
}

case class UnitPipe(
  ens:   Seq[Bit],
  block: Block[Void]
) extends Op[Void] {
  def mirror(f:Tx) = UnitPipe.staged(f(ens),f.tx(block))
}
