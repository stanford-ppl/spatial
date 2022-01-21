//package spatial.transform.debug
//
//import argon._
//import argon.lang.Bits
//import argon.transform.MutateTransformer
//import spatial.lang.{DRAM, SRAM}
//import spatial.node.{AccelScope, SRAMNew}
//import spatial.traversal.AccelTraversal
//
//import spatial.util.TransformUtils._
//import spatial.metadata.debug._
//import spatial.metadata.memory._
//
//case class SRAMDumper(IR: State, en: Boolean) extends MutateTransformer with AccelTraversal {
//  // Maps SRAM -> DRAM for debugging
//  private var dumpPairs: Map[Sym[_], Sym[_]] = null
//
//  private def visitAccel(sym: Sym[_], accel: AccelScope): Sym[_] = {
//    // gather all SRAMs which need to be dumped at the end
//    val dumpedSRAMs = accel.blocks.flatMap(_.nestedStms).filter(_.shouldDumpFinal)
//    dbgs(s"Dumping: $dumpedSRAMs")
//
//    // for each SRAM that needs to be dumped, at the end of its lifespan, stage a DRAM transfer.
//    // stage the corresponding DRAM
//    dumpedSRAMs.flatMap(makeSymOpPair) foreach {
//      case (sym, sr:SRAMNew[_, _]) =>
//        type T = sr.A.R
//        implicit def bitsEV: Bits[T] = sr.A
//        val size = sr.dims.reduce {_ * _}
//
//        // Stage this outside of the Accelscope
//        val dram = DRAM[T](size)
//        dram.explicitName = sym.explicitName.getOrElse(sym.toString + "_DumpDRAM")
//        dumpPairs += (sym -> dram)
//    }
//
//    val result = stageWithFlow(AccelScope(visitBlock(accel.block))){lhs2 => transferData(sym, lhs2)}
//
//    dumpPairs = null
//    result
//  }
//
//  override def inlineBlock[T](b: Block[T]): Sym[T] = {
//    val result = super.inlineBlock(b)
//
//    // If the block contains an SRAM that we're dumping, stage the dump.
//
//    result
//  }
//
//  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
//    case acc:AccelScope => inAccel{ visitAccel(lhs, acc) }
//
//    case _ => super.transform(lhs,rhs)
//  }).asInstanceOf[Sym[A]]
//}
