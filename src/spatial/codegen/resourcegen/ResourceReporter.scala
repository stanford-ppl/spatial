package spatial.codegen.resourcegen

import argon._
import argon.codegen.FileDependencies
import spatial.codegen.naming._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

case class ResourceReporter(IR: State)  extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "reports"
  override val ext: String = "rpt"

  override protected def emitEntry(block: Block[_]): Unit = { gen(block) }

  private def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op: SRAMNew[_,_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product} >? ${spatialConfig.sramThreshold}")
    case op: FIFONew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product} >? ${spatialConfig.sramThreshold}")
    case op: LIFONew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product} >? ${spatialConfig.sramThreshold}")
    case op: LineBufferNew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product} >? ${spatialConfig.sramThreshold}")
    case op: RegFileNew[_,_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product}")
    case op: RegNew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x 1")
    case op: FIFORegNew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x 1")
    case op: LUTNew[_,_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product}")
    case op: MergeBufferNew[_] => emit(s"${lhs}: ${bitWidth(lhs.tp.typeArgs.head)} bits x ${lhs.constDims.product}")
    case AccelScope(func)     => inAccel{ rhs.blocks.foreach{blk => gen(blk) } }
    case _:Control[_] => rhs.blocks.foreach{blk => gen(blk)}
    case _ => rhs.blocks.foreach{blk => gen(blk) }
  }


}
