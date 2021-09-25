package spatial.transform.stream

import argon._
import argon.lang.{I32, Idx}
import argon.lang.types.Bits
import argon.transform.MutateTransformer
import spatial.lang.{SRAM, SRAMN}
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.mutable
import spatial.metadata.memory._

case class StreamBufferExpansion(IR: argon.State) extends MutateTransformer with AccelTraversal {

  def shouldExpand(sym: Sym[_]): Boolean = {
    sym.isSRAM && sym.bufferAmount.getOrElse(1) > 1
  }

  def expandMem(mem: Sym[_]) = mem.op match {
    case Some(srn@SRAMNew(dims)) =>
      dbgs(s"Expanding SRAM: $mem = $srn")
      val bufferAmount = mem.bufferAmountOr1
      val newDims = Seq(I32(bufferAmount)) ++ dims
      type A = srn.A.R
      lazy implicit val bitsEV: Bits[A] = srn.A
      implicit def ctx: SrcCtx = mem.ctx
      val newMem = stageWithFlow(SRAMNew[A, SRAMN](newDims)) { nm => transferData(mem, nm) }
      newMem.r = dims.size + 1
      transferData(mem, newMem)
      newMem.fullybankdim(0)
      newMem.bufferAmount = None
//      mem.getInstance match {
//        case Some(memory) =>
//          val Ns = Seq(bufferAmount) ++ memory.nBanks
//          val Bs = Seq(1) ++ memory.Bs
//          val alphas = Seq(1) ++ memory.alphas
//          val Ps = Seq(1) ++ memory.Ps
//          dbgs(s"Inferring banking: $Ns, $Bs, $alphas, $Ps")
//          newMem.forcebank(Ns, Bs, alphas, Some(Ps))
//        case None =>
//          dbgs(s"Could not infer memory banking for $newMem from $mem")
//          newMem.fullybankdim(0)
//      }
      dbgs(s"NewMem: $newMem = ${newMem.op.get}")
      newMem
  }

  def expandWriter(sym: Sym[_], writer: Writer[_]) = {
    dbgs(s"Expanding Writer: $sym = $writer")
    val insertedDim = sym.bufferIndex.get.asInstanceOf[Idx]
    writer.mem match {
      case sr:SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A
        val dataAsBits = f(writer.data).asInstanceOf[Bits[A]]
        implicit def ctx: SrcCtx = sym.ctx
        val newWrite = stage(SRAMWrite(f(writer.mem).asInstanceOf[SRAM[A, SRAMN]], dataAsBits, Seq(insertedDim) ++ f(writer.addr), f(writer.ens)))
        newWrite
    }
  }

  def expandReader(sym: Sym[_], reader: Reader[_, _]) = {
    dbgs(s"Expanding Reader: $sym = $reader")
    val insertedDim = sym.bufferIndex.get.asInstanceOf[Idx]
    reader.mem match {
      case sr: SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A
        implicit def ctx: SrcCtx = sym.ctx
        val result = stage(SRAMRead(f(reader.mem).asInstanceOf[SRAM[A, SRAMN]], Seq(insertedDim) ++ f(reader.addr), reader.ens))
        result
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case AccelScope(_) => inAccel {
      super.transform(lhs, rhs)
    }
    case writer: SRAMWrite[_, _] if lhs.bufferIndex.isDefined => expandWriter(lhs, writer)
    case reader: SRAMRead[_, _] if lhs.bufferIndex.isDefined => expandReader(lhs, reader)
    case mem: SRAMNew[_, _] if lhs.bufferAmount.isDefined => expandMem(lhs)
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
