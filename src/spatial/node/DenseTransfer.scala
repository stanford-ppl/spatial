package spatial.node

import core._
import forge.tags._

import spatial.lang._

/** A dense transfer between on-chip and off-chip memory
  * If isLoad is true, this is a transfer from off-chip to on-chip.
  * Otherwise, this is a transfer from on-chip to off-chip.
  *
  * @tparam A The type of the elements being loaded/stored
  * @tparam Dram The type of the off-chip memory
  * @tparam Local The type of the on-chip memory
  * @param ens Explicit enable signals for this transfer
  * @param dram The instance of the off-chip memory
  * @param local The instance of the on-chip memory
  * @param dramOfs Offsets into the off-chip memory for the start of this transfer
  * @param localOfs Offsets into the on-chip memory for the start of this transfer
  * @param tileDims Dimensions of the tile of the off-chip memory
  * @param localDims Dimensions of the tile of the on-chip memory
  * @param strides
  * @param units Flags for whether size of given dimension is 1
  * @param p The parallelization factor of this load
  * @param isLoad If true this is a load from off-chip (if true), otherwise this is a store to off-chip
  * @param iters Iterators to use in iterating over dimensions
  * @param bA Type evidence for the element
  * @param tL Type evidence for the on-chip memory
  * @param tD Type evidence for the off-chip memory
  */
@op case class DenseTransfer[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    ens:       Set[Bit],
    dram:      Dram[A],
    local:     Local[A],
    dramOfs:   Seq[Idx],
    localOfs:  Seq[Idx],
    tileDims:  Seq[Idx],
    localDims: Seq[Idx],
    strides:   Seq[Idx],
    units:     Seq[Boolean],
    p:         I32,
    isLoad:    Boolean,
    iters:     List[I32]
  )(implicit
    val bA: Bits[A],
    val tL: Type[Local[A]],
    val tD: Type[Dram[A]])
  extends MemTransfer {
  def isStore: Boolean = !isLoad
  var isAlign = false
}


@op case class SparseTransfer[A,Local[T]<:LocalMem[T,Local],Addr[T]<:LocalMem[T,Addr],Dram[T]<:DRAM[T,Dram]](
  ens:      Set[Bit],
  dram:     Dram[A],
  local:    Local[A],
  addrs:    Addr[Idx],
  localOfs: Idx,
  addrOfs:  Idx,
  size:     Idx,
  p:        I32,
  isLoad:   Boolean,
  i:        I32
)(implicit
    val bA:   Bits[A],
    val tL:   Type[Local[A]],
    val tA:   Type[Addr[Idx]],
    val tD:   Type[Dram[A]])
  extends MemTransfer {
  def isStore: Boolean = !isLoad
  def iters = Seq(i)
}
