package spatial.node

import argon._
import forge.tags._

import spatial.lang._

/** A dense transfer between on-chip and off-chip memory
  * If isLoad is true, this is a transfer from off-chip to on-chip.
  * Otherwise, this is a transfer from on-chip to off-chip.
  * If isAlign
  *
  * @tparam A The type of the elements being loaded/stored
  * @tparam Dram The type of the off-chip memory
  * @tparam Local The type of the on-chip memory
  * @param dram The instance of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param isLoad If true this is a load from off-chip (if true), otherwise this is a store to off-chip
  * @param forceAlign If true, this forces the transfer to be aligned with the DRAM's burst size.
  * @param ens Explicit enable signals for this transfer
  * @param A Type evidence for the element
  * @param Local Type evidence for the on-chip memory
  * @param Dram Type evidence for the off-chip memory
  */
@op case class DenseTransfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    isLoad:     Boolean,
    forceAlign: Boolean = false,
    ens:        Set[Bit] = Set.empty
  )(implicit
    val A: Bits[A],
    val Local: Type[Local[A]],
    val Dram: Type[Dram[A]])
  extends EarlyBlackBox {
  def isStore: Boolean = !isLoad

  override def effects: Effects = if (isStore) Effects.Writes(dram) else Effects.Writes(local)
}

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
  extends EarlyBlackBox {
  def isScatter: Boolean = !isGather

  override def effects: Effects = if (isScatter) Effects.Writes(dram) else Effects.Writes(local)
}

