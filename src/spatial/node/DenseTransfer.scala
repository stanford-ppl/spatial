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
    val A:     Bits[A],
    val Local: Type[Local[A]],
    val Dram:  Type[Dram[A]])
  extends EarlyBlackBox[Void] {
    def isStore: Boolean = !isLoad

    override def effects: Effects = if (isStore) Effects.Writes(dram) else Effects.Writes(local)
    @rig def lower(): Void = {
      if (isLoad) DenseTransfer.load(dram,local,forceAlign,ens)
      else        DenseTransfer.store(dram,local,forceAlign,ens)
    }
}

object DenseTransfer {
  @rig private def common[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    forceAlign: Boolean,
    ens:        Set[Bit]
  )(transfer: => Void)(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Dram:  Type[Dram[A]]
  ): Void = {



  }

  @rig def load[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    forceAlign: Boolean,
    ens:        Set[Bit]
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Dram:  Type[Dram[A]]
  ): Void = common(dram,local,forceAlign,ens){

  }

  @rig def store[A,Dram[T]<:DRAM[T,Dram],Local[T]<:LocalMem[T,Local]](
    dram:       Dram[A],
    local:      Local[A],
    forceAlign: Boolean,
    ens:        Set[Bit]
  )(implicit
    A:     Bits[A],
    Local: Type[Local[A]],
    Dram:  Type[Dram[A]]
  ): Void = common(dram,local,forceAlign,ens){

  }
}
