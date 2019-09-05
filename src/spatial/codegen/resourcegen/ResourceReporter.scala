package spatial.codegen.resourcegen

import scala.collection.mutable

import argon._
import argon.codegen.FileDependencies
import spatial.codegen.naming._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

case class ResourceReporter(IR: State) extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "reports"
  override val ext: String = "json"

  override protected def emitEntry(block: Block[_]): Unit = {
    gen(block)
  }

  private def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }

  var fixOp: Int = 0

  override def emitHeader(): Unit = {
    super.emitHeader()
    emit("{")
  }

  val dataMap = mutable.Map[String, mutable.Map[String, String]]()

  override def emitFooter(): Unit = {
    super.emitFooter()
    dataMap.foreach {
      entry => {
        emit(s"""\t"${entry._1}": {""")
        val last_index = entry._2.size - 1;
        entry._2.toList.zipWithIndex.foreach {
          tup_ind => {
            val comma = if (tup_ind._2 != last_index) "," else ""
            emit(s"""\t\t"${tup_ind._1._1}": [${tup_ind._1._2}]${comma}""")
          }
        }
        emit("\t},")
      }
    }
    emit(s"""\t"fixed_ops": $fixOp""")
    emit("}")
  }

  def emitMem(lhs: Sym[_], tp: String, dims: Seq[Int], padding: Seq[Int], depth: Int) = {
    dataMap.getOrElseUpdate(tp, mutable.Map[String, String]()) += (
      src"${lhs}" -> src"""${bitWidth(lhs.tp.typeArgs.head)}, ${dims}, ${padding}, ${depth}""")
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Seq[_] =>
      s"[${p.map(quoteOrRemap).mkString(",")}]"
    case _ => super.quoteOrRemap(arg)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    rhs match {
      case AccelScope(func) => inAccel {
        spatialConfig.enGen = true
        gen(func)
      }
      case _ =>
        if (inHw) {
          countResource(lhs, rhs)
        }
        rhs.blocks.foreach { blk => gen(blk) }
    }
  }

  def countResource(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op: SRAMNew[_, _] =>
      emitMem(lhs, "bram", lhs.constDims, lhs.padding, lhs.instance.depth)
    case op: FIFONew[_] =>
      emitMem(lhs, "bram", lhs.constDims, lhs.padding, lhs.instance.depth)
    case op: LIFONew[_] =>
      emitMem(lhs, "bram", lhs.constDims, lhs.padding, lhs.instance.depth)
    case op: LineBufferNew[_] =>
      emitMem(lhs, "bram", lhs.constDims, lhs.padding, lhs.instance.depth)
    case op: RegFileNew[_, _] =>
      emitMem(lhs, "bram", lhs.constDims, lhs.padding, lhs.instance.depth)
    case op: RegNew[_] =>
      emitMem(lhs, "reg", Seq(1), Seq(0), lhs.instance.depth)
    case op: FIFORegNew[_] =>
      emitMem(lhs, "reg", Seq(1), Seq(0), lhs.instance.depth)
    case op: LUTNew[_, _] =>
      emitMem(lhs, "reg", lhs.constDims, Seq(0), 1)
    case op: MergeBufferNew[_] =>
      emitMem(lhs, "reg", lhs.constDims, Seq(0), 1)

    case FixInv(x) => fixOp += 1
    case FixAdd(x, y) => fixOp += 1
    case FixSub(x, y) => fixOp += 1
    case FixMul(x, y) => fixOp += 1
    case FixDiv(x, y) => fixOp += 1
    case FixRecip(x) => fixOp += 1
    case FixMod(x, y) => fixOp += 1
    case FixAnd(x, y) => fixOp += 1
    case FixOr(x, y) => fixOp += 1
    case FixLst(x, y) => fixOp += 1
    case FixLeq(x, y) => fixOp += 1
    case FixXor(x, y) => fixOp += 1
    case FixSLA(x, y) => fixOp += 1
    case FixSRA(x, y) => fixOp += 1
    case FixSRU(x, y) => fixOp += 1
    case SatAdd(x, y) => fixOp += 1
    case SatSub(x, y) => fixOp += 1
    case SatMul(x, y) => fixOp += 1
    case SatDiv(x, y) => fixOp += 1
    case UnbMul(x, y) => fixOp += 1
    case UnbDiv(x, y) => fixOp += 1
    case UnbSatMul(x, y) => fixOp += 1
    case UnbSatDiv(x, y) => fixOp += 1
    case FixNeq(x, y) => fixOp += 1
    case FixEql(x, y) => fixOp += 1
    case FixMax(x, y) => fixOp += 1
    case FixMin(x, y) => fixOp += 1
    case FixToFlt(x, fmt) => fixOp += 1
    case FixToText(x,format) => fixOp += 1
    case TextToFix(x, _) => fixOp += 1
    case FixRandom(Some(max)) => fixOp += 1
    case FixRandom(None) => fixOp += 1
    case FixAbs(x) => fixOp += 1
    case FixFloor(x) => fixOp += 1
    case FixCeil(x) => fixOp += 1
    case FixLn(x) => fixOp += 1
    case FixExp(x) => fixOp += 1
    case FixSqrt(x) => fixOp += 1
    case FixSin(x) => fixOp += 1
    case FixCos(x) => fixOp += 1
    case FixTan(x) => fixOp += 1
    case FixSinh(x) => fixOp += 1
    case FixCosh(x) => fixOp += 1
    case FixTanh(x) => fixOp += 1
    case FixAsin(x) => fixOp += 1
    case FixAcos(x) => fixOp += 1
    case FixAtan(x) => fixOp += 1
    case FixPow(x, exp) => fixOp += 1
    case FixFMA(m1, m2, add) => fixOp += 1
    case FixRecipSqrt(x) => fixOp += 1
    case FixSigmoid(x) => fixOp += 1

    case _ =>
  }


}
