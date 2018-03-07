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




object DRAMTransfers {
  /** Internal **/
  /*@rig def dense_transfer[A:Bits,Local[T],Dram[T]<:DRAM[T,Dram]](
    tile:   DRAMDenseTile[A,Dram],
    local:  Local[A],
    isLoad: Boolean,
    isAlign: Boolean = false
  )(implicit mem: LocalMem[A,Local], mC: Type[Dram[A]]): Void = {
    // Extract range lengths early to avoid unit pipe insertion eliminating rewrite opportunities
    val dram: Dram[A]     = tile.dram
    val dramOfs: Seq[Idx] = tile.ranges.map(_.start)
    val lens: Seq[Idx]    = tile.ranges.map(_.length)
    val strides = tile.ranges.map(_.step.map(_.s).getOrElse(int32s(1)))
    val units   = tile.ranges.map(_.isUnit)
    val p       = extractParFactor(tile.ranges.last.p)

    // UNSUPPORTED: Strided ranges for DRAM in burst load/store
    if (strides.exists{case Exact(c) if (c == 1 || isLoad) => false ; case _ => true}) {
      new spatial.UnsupportedStridedDRAMError(isLoad)(ctx, state)
    } else if (strides.last match{case Exact(c) if (c == 1) => false ; case _ => true}) {
      new spatial.UnsupportedStridedDRAMError(isLoad)(ctx, state)
    }

    val localRank = mem.iterators(local).length // TODO: Replace with something else here (this creates counters)

    val iters = List.tabulate(localRank){_ => fresh[Index]}

    MUnit(op_dense_transfer(dram,local.s,ofs,lens,strides,units,p,isLoad,isAlign,iters))
  }

  @rig def sparse_transfer[T,C[T],A[_]](
    tile:   DRAMSparseTileMem[T,A],
    local:  C[T],
    isLoad: Boolean
  )(implicit mT: Type[T], bT: Bits[T], memC: Mem[T,C], mC: Type[C[T]]): MUnit = {
    implicit val mD: Type[DRAM[T]] = tile.dram.tp
    implicit val memA: Mem[Index,A] = tile.memA
    implicit val mA: Type[A[Index]] = tile.mA

    val p = extractParFactor(memA.par(tile.addrs))
    val size = tile.len.s
    val i = fresh[Index]
    MUnit(op_sparse_transfer_mem(tile.dram, local.s, tile.addrs.s, size, p, isLoad, i))
  }

  /** Constructors **/
  @rig def op_dense_transfer[T:Bits,C[T]](
    dram:   Exp[DRAM[T]],
    local:  Exp[C[T]],
    ofs:    Seq[Exp[Index]],
    lens:   Seq[Exp[Index]],
    strides:Seq[Exp[Index]],
    units:  Seq[Boolean],
    p:      Const[Index],
    isLoad: Boolean,
    isAlign: Boolean,
    iters:  List[Bound[Index]]
  )(implicit mem: Mem[T,C], mC: Type[C[T]], mD: Type[DRAM[T]]): Exp[MUnit] = {

    val node = DenseTransfer(dram,local,ofs,lens,strides,units,p,isLoad,iters)
    node.isAlign = isAlign

    val out = if (isLoad) stageWrite(local)(node)(ctx) else stageWrite(dram)(node)(ctx)
    styleOf(out) = InnerPipe
    out
  }

  @rig def op_sparse_transfer[T:Bits,C[T],A[_]](
    dram:   Exp[DRAM[T]],
    local:  Exp[C[T]],
    addrs:  Exp[A[Index]],
    size:   Exp[Index],
    p:      Const[Index],
    isLoad: Boolean,
    i:      Bound[Index]
  )(implicit memC: Mem[T,C], mC: Type[C[T]], memA: Mem[Index,A], mA: Type[A[Index]], mD: Type[DRAM[T]]) = {
    val node = SparseTransferMem(dram,local,addrs,size,p,isLoad,i)

    val out = if (isLoad) stageWrite(local)(node)(ctx) else stageWrite(dram)(node)(ctx)
    styleOf(out) = InnerPipe
    out
  }*/
}
