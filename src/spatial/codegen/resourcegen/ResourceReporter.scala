package spatial.codegen.resourcegen

import argon._
import argon.codegen.FileDependencies
import argon.node._
import spatial.codegen.naming._
import spatial.lang._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig
import models.AreaEstimator

import scala.collection.mutable

case class ResourceArea(LUT: Double, Reg: Double, BRAM: Double, DSP: Double) {
  def and(x: ResourceArea): ResourceArea = ResourceArea(x.LUT + LUT, x.Reg + Reg, x.BRAM + BRAM, x.DSP + DSP)
  override def toString: String = s"LUTs: $LUT, Regs: $Reg, BRAM: $BRAM, DSP: $DSP"
}

case class ResourceReporter(IR: State, areamodel: AreaEstimator) extends NamedCodegen with FileDependencies with AccelTraversal {
  override val lang: String = "reports"
  override val ext: String = "json"
  var depth = 0

  override protected def emitEntry(block: Block[_]): Unit = {
    gen(block)
  }

  private def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }


  override def emitHeader(): Unit = {
    super.emitHeader()
  }

  def inCtrl(lhs: Sym[_])(func: => ResourceArea): ResourceArea = {
    emit("  " * depth + s"Controller: $lhs (${lhs.ctx})")
    depth = depth + 1
    val area = func
    depth = depth - 1
    emit("  " * depth + s"$lhs total area: $area")
    area
  }

  override def emitFooter(): Unit = {
    super.emitFooter()
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Seq[_] =>
      s"[${p.map(quoteOrRemap).mkString(",")}]"
    case _ => super.quoteOrRemap(arg)
  }

  /** Returns an approximation of the cost for the given banking strategy. */
  def estimateMem(mem: Sym[_]): ResourceArea = { //(banking: Seq[Banking], depth: Int, rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): DUPLICATE = {
    val depth = mem.instance.depth
    val allDims = mem.constDims
    val allB = mem.instance.Bs
    val allAlpha = mem.instance.alphas
    val allN = mem.instance.nBanks
    val allP = mem.instance.Ps
    val nBanks = if (mem.isLUT | mem.isRegFile) allDims else mem.instance.nBanks

    val histR: Map[Int, Int] = mem.readers.toList.flatMap{x => x.residualGenerators}.zip(mem.readers.toList.flatMap{x => if (x.getPorts.isDefined) x.port.broadcast else List.fill(x.residualGenerators.size)(0)}).collect{case (rg,b) if b == 0 => rg}.groupBy{lane => lane.zipWithIndex.map{case (r,j) => r.expand(nBanks(j)).size}.product}.map{case(k,v) => k -> v.size}
    val histW: Map[Int, Int] = mem.writers.toList.flatMap{x => x.residualGenerators}.zip(mem.writers.toList.flatMap{x => if (x.getPorts.isDefined) x.port.broadcast else List.fill(x.residualGenerators.size)(0)}).collect{case (rg,b) if b == 0 => rg}.groupBy{lane => lane.zipWithIndex.map{case (r,j) => r.expand(nBanks(j)).size}.product}.map{case(k,v) => k -> v.size}

    val histCombined: Map[Int, (Int,Int)] = histR.map{case (width, siz) => width -> (siz, histW.getOrElse(width, 0)) } ++ histW.collect{case (width, siz) if !histR.contains(width) => width -> (0,siz) }
    val histRaw = histCombined.toList.sortBy(_._1).map{x => List(x._1, x._2._1, x._2._2)}.flatten

    mem.asInstanceOf[Sym[_]] match {
      case m:SRAM[_,_] =>
        val luts = areamodel.estimateMem("LUTs", "SRAMNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val ffs = areamodel.estimateMem("FFs", "SRAMNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val bram = areamodel.estimateMem("RAMB18", "SRAMNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "SRAMNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw)
        ResourceArea(luts, ffs, bram, 0)
      case m:RegFile[_,_] =>
        val luts = areamodel.estimateMem("LUTs", "RegFileNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val ffs = areamodel.estimateMem("FFs", "RegFileNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val bram = areamodel.estimateMem("RAMB18", "RegFileNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "RegFileNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        ResourceArea(luts, ffs, bram, 0)
      case m:LineBufferNew[_] =>
        val luts = areamodel.estimateMem("LUTs", "LineBufferNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val ffs = areamodel.estimateMem("FFs", "LineBufferNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val bram = areamodel.estimateMem("RAMB18", "LineBufferNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "LineBufferNew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        ResourceArea(luts, ffs, bram, 0)
      case m:FIFONew[_] =>
        val luts = areamodel.estimateMem("LUTs", "FIFONew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val ffs = areamodel.estimateMem("FFs", "FIFONew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val bram = areamodel.estimateMem("RAMB18", "FIFONew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "FIFONew", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        ResourceArea(luts, ffs, bram, 0)
      case m:RegNew[_] => ResourceArea(0,1,0,0)
      case _ =>
        val luts = areamodel.estimateMem("LUTs", "", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val ffs = areamodel.estimateMem("FFs", "", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        val bram = areamodel.estimateMem("RAMB18", "", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "", allDims, bitWidth(mem.tp.typeArgs.head), depth, allB, allN, allAlpha, allP, histRaw)
        ResourceArea(luts, ffs, bram, 0)

    }
  }

  def estimateArea(block: Block[_]): ResourceArea = {
    val xs = block.stms.map{
      case x@Op(_: Control[_]) =>
        inCtrl(x){
          x.blocks.map{estimateArea}.fold(ResourceArea(0,0,0,0)){_.and(_)}
        }
      case x@Op(_: MemAlloc[_,_]) =>
        val area = estimateMem(x)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixMul(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixMul", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixMul", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixMul", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixMul", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixMul", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixDiv(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixDiv", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixDiv", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixDiv", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixDiv", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixDiv", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixMod(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixMod", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixMod", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixMod", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixMod", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixMod", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixSub(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixSub", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixSub", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixSub", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixSub", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixSub", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixAdd(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixAdd", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixAdd", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixAdd", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixAdd", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixAdd", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixFMA(a,b,c)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixFMA", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixFMA", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixFMA", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixFMA", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixFMA", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x@Op(_@FixToFix(a,b)) =>
        val l = areamodel.estimateArithmetic("LUTs", "FixToFix", List(0,0, bitWidth(x.tp),0,1))
        val f = areamodel.estimateArithmetic("FFs", "FixToFix", List(0,0, bitWidth(x.tp),0,1))
        val br = areamodel.estimateArithmetic("RAMB18", "FixToFix", List(0,0, bitWidth(x.tp),0,1)) + areamodel.estimateArithmetic("RAMB32", "FixToFix", List(0,0, bitWidth(x.tp),0,1))
        val d = areamodel.estimateArithmetic("DSPs", "FixToFix", List(0,0, bitWidth(x.tp),0,1))
        val area = ResourceArea(l,f,br, d)
        emit("  " * depth + s"$x (${x.name}, ${x.rhs}) - $area (${x.ctx}}")
        area
      case x =>
        val area = x.blocks.map(estimateArea).fold(ResourceArea(0,0,0,0)){_.and(_)}
        if (area != ResourceArea(0,0,0,0)) Console.println(s"Problem collecting area info on $x ${x.rhs}")
        area


    }
    xs.fold(ResourceArea(0,0,0,0)){_.and(_)}
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    rhs match {
      case AccelScope(func) => inAccel { inCtrl(lhs) {
        spatialConfig.enGen = true
        val area = estimateArea(func)
        emit(s"Total area: $area")
        spatialConfig.enGen = false
        area
      }}
      case _ =>
    }
  }



}
