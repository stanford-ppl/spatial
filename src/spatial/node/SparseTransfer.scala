package spatial.node

import argon._
import forge.tags._

import spatial.lang._

/** A sparse transfer between on-chip and off-chip memory.
  * If isGather is true, this is a gather from off-chip memory to on-chip.
  * Otherwise, this is a scatter from on-chip memory to off-chip memory.
  *
  * @tparam A The type of elements being loaded/stored
  * @tparam Local The type of the on-chip memory.
  * @param dram A sparse view of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param isGather If true, this is a gather from off-chip. Otherwise, this is a scatter to off-chip.
  * @param ens Explicit enable signals for this transfer
  * @param bA Type evidence for the element
  * @param tL Type evidence for the on-chip memory.
  */
@op case class SparseTransfer[A,Local[T]<:LocalMem[T,Local]](
    dram:     DRAMSparseTile[A],
    local:    Local[A],
    isGather: Boolean,
    ens:      Set[Bit] = Set.empty,
  )(implicit
    val bA:   Bits[A],
    val tL:   Type[Local[A]])
  extends EarlyBlackBox[Void] {
  def isScatter: Boolean = !isGather

  override def effects: Effects = if (isScatter) Effects.Writes(dram) else Effects.Writes(local)
  @rig def lower(): Void = {
    if (isScatter) SparseTransfer.scatter(dram,local,ens)
              else SparseTransfer.gather(dram,local,ens)
  }
}

object SparseTransfer {

  @rig def scatter[A,Local[T]<:LocalMem[T,Local]](
    dram:  DRAMSparseTile[A],
    local: Local[A],
    ens:   Set[Bit]
  )(implicit
    bA:   Bits[A],
    tL:   Type[Local[A]]
  ): Void = {

  }

  @rig def gather[A,Local[T]<:LocalMem[T,Local]](
    dram:  DRAMSparseTile[A],
    local: Local[A],
    ens:   Set[Bit]
  )(implicit
    bA:   Bits[A],
    tL:   Type[Local[A]]
  ): Void = {

  }

}