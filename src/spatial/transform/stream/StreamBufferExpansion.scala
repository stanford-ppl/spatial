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

case class StreamBufferExpansion(IR: State) extends MutateTransformer with AccelTraversal {

  def shouldExpand(sym: Sym[_]) = {
    sym.isSRAM && sym.bufferAmount.getOrElse(1) > 1
  }

  def expandMem(mem: Sym[_]) = mem.op match {
    case Some(srn@SRAMNew(dims)) =>
      dbgs(s"Expanding SRAM: $mem")
      val bufferAmount = mem.bufferAmountOr1
      val newDims = Seq(I32(bufferAmount)) ++ dims
      type A = srn.A.R
      lazy implicit val bitsEV: Bits[A] = srn.A
      val newMem = stage(SRAMNew[A, SRAMN](newDims))
      newMem.r = dims.size + 1
      newMem.fullybankdim(0)
      mem.explicitName match {
        case Some(name) => newMem.explicitName = name
      }
      register(mem -> newMem)
      newMem
  }

  def expandWriter(sym: Sym[_], writer: Writer[_]) = {
    dbgs(s"Expanding Writer: $sym")
    val insertedDim = sym.bufferIndex.get.asInstanceOf[Idx]
    writer.mem match {
      case sr:SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A
        val dataAsBits = writer.data.asInstanceOf[Bits[A]]
        val newWrite = stage(SRAMWrite(f(writer.mem).asInstanceOf[SRAM[A, SRAMN]], dataAsBits, Seq(insertedDim) ++ writer.addr, writer.ens))
        register(sym -> newWrite)
        newWrite
    }
  }

  def expandReader(sym: Sym[_], reader: Reader[_, _]) = {
    dbgs(s"Expanding Reader: $sym")
    val insertedDim = sym.bufferIndex.get.asInstanceOf[Idx]
    reader.mem match {
      case sr: SRAM[_, _] =>
        type A = sr.A.R
        lazy implicit val bitsEV: Bits[A] = sr.A
        val result = stage(SRAMRead(f(reader.mem).asInstanceOf[SRAM[A, SRAMN]], Seq(insertedDim) ++ reader.addr, reader.ens))
        register(sym -> result)
        result
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    (rhs match {
      case AccelScope(_) => inAccel {
        super.transform(lhs, rhs)
      }

        // For SRAMs which are Stream-Buffered
      case _ if shouldExpand(lhs) => expandMem(lhs)
      case wr: Writer[_] if shouldExpand(wr.mem) => expandWriter(lhs, wr)
      case rd: Reader[_, _] if shouldExpand(rd.mem) => expandReader(lhs, rd)

      case _ => super.transform(lhs, rhs)
    }).asInstanceOf[Sym[A]]
  }
}
