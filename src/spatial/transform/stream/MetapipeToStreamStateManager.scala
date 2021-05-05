package spatial.transform.stream

import argon.Sym

import scala.collection.mutable

trait MetapipeToStreamStateManager {
  // For Duplicated Memories
  // Reader -> Memory -> FIFO
  val duplicationReadFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], Sym[_]]]()

  // Writer -> Memory -> FIFOs
  val duplicationWriteFIFOs = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]]()

  // For Buffered Memories
  // Reader -> OriginalMemory -> Seq[New Memory]
  val bufferReadMemories = mutable.Map[Sym[_], mutable.Map[Sym[_], Seq[Sym[_]]]]()

  // OriginalMemory -> writer -> Seq[Seq[New Memory]]
  val bufferWriteMemories = mutable.Map[Sym[_], mutable.Map[Sym[_], mutable.ArrayBuffer[Seq[Sym[_]]]]]()
}
