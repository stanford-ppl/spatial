package spatial.targets

import argon._
import argon.node._
import forge.tags._
import utils.implicits.collections._
import models._

import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._

abstract class AreaModel(target: HardwareTarget, mModel: AreaEstimator) extends SpatialModel[AreaFields](target) {
  final def mlModel: AreaEstimator = mModel

  val FILE_NAME: String = target.name.replaceAll(" ", "_") + "_Area.csv"
  val RESOURCE_NAME: String = "Area"
  final def FIELDS: Array[String] = target.AFIELDS
  final implicit def RESOURCE_FIELDS: AreaFields[Double] = target.AREA_FIELDS
  final implicit def MODEL_FIELDS: AreaFields[NodeModel] = target.AMODEL_FIELDS

  def RegArea(n: Int, bits: Int): Area = model("Reg")("b"->bits, "d"->1) * n
  def MuxArea(n: Int, bits: Int): Area = model("Mux")("b"->bits) * n // TODO: Not sure if this is always right

  final def NoArea: Area = Area.empty[Double]

  @stateful def areaOf(e: Sym[_], inHwScope: Boolean, inReduce: Boolean): Area = e.op match {
    case Some(d) => areaOf(e, d, inHwScope, inReduce)
    case None => NoArea
  }
  @stateful final def areaOf(e: Sym[_], d: Op[_], inHwScope: Boolean, inReduce: Boolean): Area = {
    if (!inHwScope) NoArea else if (inReduce) areaInReduce(e, d) else areaOfNode(e, d)
  }

  @stateful def wordWidth(mem: Sym[_]): Int = {
    mem.tp.typeArgs.headOption.collect{case Bits(bt) => bt.nbits }.getOrElse(1)
  }

  @stateful def accessUnrollCount(mem: Sym[_], a: Sym[_]): Int = {
    var parent = a.parent.s
    val stop = mem.parent.s
    var par = 1
    while (parent.isDefined && stop.isDefined && parent != stop) {
      parent.get match {
        case Op(OpForeach(_, cchain, _, _, _)) =>
          par = par * cchain.constPars.product
        case Op(OpReduce(_, cchain, _, _, _, _, _, _, _, _, _)) =>
          par = par * cchain.constPars.product
        case Op(OpMemReduce(_, cchainMap, cchainRed, _, _, _, _, _, _, _, _, _, _, _)) =>
          par = par * (cchainMap.constPars.product + cchainRed.constPars.product) // TODO: This is definitely wrong, need to check stage of access
        case _ =>
      }
      parent = parent.get.parent.s
    }
    par
  }

  @stateful def areaOfMem(mem: Sym[_], name: String, dims: Seq[Int]): Area = {
    val depth = try {
      (mem.readers ++ mem.writers).pairs.map { case (a, b) =>
        val ancA = a.ancestors.collect { case x if x.s.isDefined => x.s.get }
        val ancB = b.ancestors.collect { case x if x.s.isDefined => x.s.get }
        val lca = (ancA intersect ancB).headOption
        if (lca.isDefined && lca.get.isOuterPipeControl) {
          val childA = lca.get.children.map(_.s.get) intersect ancA
          val childB = lca.get.children.map(_.s.get) intersect ancB
          scala.math.abs(lca.get.children.map(_.s.get).indexOf(childA) - lca.get.children.map(_.s.get).indexOf(childB))
        } else 0
      }.toList.lastOption.getOrElse(0)
    } catch { case _: Throwable => 1 }
    // Assume flat banking so that we don't need to run through banking analysis, N = biggest unrolled group
    val rGroups: Seq[Int] = mem.readers.map{r => accessUnrollCount(mem,r)}.toSeq
    val wGroups: Seq[Int] = mem.writers.map{w => accessUnrollCount(mem,w)}.toSeq
    val B = Seq(1)
    val N = Seq((rGroups ++ wGroups).sorted.lastOption.getOrElse(1))
    val histR = rGroups.groupBy{v => v}.toList.map{x => if (x._1 == N.head) (1 -> x._2.sum) else (N.head -> x._2.sum)}.groupBy{v => v._1}.map{x => x._1 -> x._2.map(_._2).sum}.toMap
    val histW = wGroups.groupBy{v => v}.toList.map{x => if (x._1 == N.head) (1 -> x._2.sum) else (N.head -> x._2.sum)}.groupBy{v => v._1}.map{x => x._1 -> x._2.map(_._2).sum}.toMap
    val histMap: Map[Int, (Int,Int)] = histR.map{case (width, siz) => (width -> (siz, histW.getOrElse(width, 0)))} ++ histW.collect{case (width, siz) if !histR.contains(width) => (width -> (0,siz))}
    val hist = histMap.toList.sortBy(_._1).map{x => List(x._1, x._2._1, x._2._2)}.flatten
    val alpha = Seq.tabulate(dims.size){i => 1}
    val P = Seq.tabulate(dims.size){i => 1} // Very wrong
    val bitWidth = wordWidth(mem)
    val luts = mlModel.estimateMem("LUTs", name, dims, bitWidth, depth, B, N, alpha, P, hist) * (depth + 1)
    val ffs = mlModel.estimateMem("FFs", name, dims, bitWidth, depth, B, N, alpha, P, hist) * (depth + 1)
//    val ram18 = scala.math.ceil(mlModel.estimateMem("RAMB18", name, dims, bitWidth, depth, B, N, alpha, P, hist)).toInt
//    val ram36 = scala.math.ceil(mlModel.estimateMem("RAMB36", name, dims, bitWidth, depth, B, N, alpha, P, hist)).toInt
    val ram36 = N.head * (1 max scala.math.ceil((dims.product / (N.head * 36000))).toInt) * (depth + 1) // TODO: models give wildly large numbers, ram = N should be more realistic for now
    val ram18 = 0
    if (name == "NoImpl") NoArea else Area(("RAM18", ram18), ("RAM36", ram36), ("Regs", ffs), ("Slices", luts))
  }

  @stateful def rawRegArea(reg: Sym[_]): Area = model("Reg")("b" -> wordWidth(reg))

  @stateful def areaOfReg(reg: Sym[_]): Area = {
    if (reg.getDuplicates.isDefined) {
      val instances = reg.duplicates
      instances.map{instance =>
        rawRegArea(reg)
      }.fold(NoArea){_+_}
    } else NoArea
  }

  @stateful def SRAMArea(width: Int, depth: Int, resource: MemoryResource): Area = {
    resource.area(width,depth)
  }

  @stateful def memoryBankDepth(mem: Sym[_], instance: Memory): Int = {
    val width = wordWidth(mem)
    val dims  = mem.constDims
    memoryBankDepth(width, dims, instance)
  }

  @stateful def memoryBankDepth(width: Int, dims: Seq[Int], instance: Memory): Int = {
    instance.bankDepth(dims)
  }

  @stateful def rawMemoryBankArea(mem: Sym[_], instance: Memory, resource: MemoryResource): Area = {
    val width = wordWidth(mem)
    val dims  = mem.constDims
    rawMemoryBankArea(width, dims, instance, resource)
  }
  @stateful def rawMemoryBankArea(width: Int, dims: Seq[Int], instance: Memory, resource: MemoryResource): Area = {
    val totalElements = dims.product
    val nBanks = instance.totalBanks
    val bankDepth = memoryBankDepth(width, dims, instance)
    val resourcesPerBank = SRAMArea(width, bankDepth, resource)

    log(s"Memory")
    log(s"------------------------")
    log(s"Total elements:   $totalElements")
    log(s"Word width:       $width")
    log(s"# of banks:       $nBanks")
    log(s"Elements / Bank:  $bankDepth")
    log(s"Resources / Bank: $resourcesPerBank")
    resourcesPerBank
  }

  @stateful def rawMemoryArea(mem: Sym[_], instance: Memory, resource: MemoryResource): Area = {
    val width = wordWidth(mem)
    val dims  = mem.constDims
    rawMemoryArea(width, dims, instance, resource)
  }
  @stateful def rawMemoryArea(width: Int, dims: Seq[Int], instance: Memory, resource: MemoryResource): Area = {
    val nBanks = instance.totalBanks
    val bufferDepth = instance.depth
    val resourcesPerBank   = rawMemoryBankArea(width, dims, instance, resource)
    val resourcesPerBuffer = resourcesPerBank * nBanks

    log(s"Buffer depth:     $bufferDepth")
    log(s"Buffer resources: $resourcesPerBuffer")
    resourcesPerBuffer * bufferDepth
  }


  @stateful def nDups(e: Sym[_]): Int = if (e.getDuplicates.isDefined) e.duplicates.length else 0

  @stateful def areaInReduce(e: Sym[_], d: Op[_]): Area = areaOfNode(e, d)

  @stateful def areaOfNode(lhs: Sym[_], rhs: Op[_]): Area = rhs match {
    /** Non-synthesizable nodes */
    case op:Primitive[_] if !op.canAccel => NoArea

    /** Zero area cost */
    case Transient(_)       => NoArea
    case _:DRAMHostNew[_,_]     => NoArea
    case DRAMAddress(_)  => NoArea
    case _:SwitchCase[_]    => NoArea

    case x:MemAlloc[_,_] if !lhs.isRemoteMem =>
      val name = lhs match {case Op(_:SRAMNew[_,_]) => "SRAMNew"; case Op(_:RegFile[_,_]) => "RegFileNew"; case Op(_:LineBufferNew[_]) => "LineBufferNew"; case _ => "NoImpl"}
      val a = areaOfMem(lhs, name, x.dims.map(_.toInt))
      a
    case _:MemAlloc[_,_] if lhs.isRemoteMem => NoArea
    case op:Accessor[_,_]         => NoArea // Included already in MemAlloc model
    case op:UnrolledAccessor[_,_] => NoArea // Included already in MemAlloc model
    case op:StatusReader[_]       => NoArea

    case DelayLine(size,data) => areaOfDelayLine(size,nbits(data),1)

    // Just count DSPs for arithmetic for now
    case op@FixMul(a,b) if !a.isConst && !b.isConst => Area("DSPs" -> 1)
    case op@FixMul(a,Const(b)) if !b.isPow2 => Area("DSPs" -> 1)
    case op@FixDiv(a,b) if !a.isConst && !b.isConst => Area("DSPs" -> 1)
    case op@FixDiv(a,Const(b)) if !b.isPow2 => Area("DSPs" -> 1)
    case op@FixMod(a,b) if !a.isConst && !b.isConst => Area("DSPs" -> 1)
    case op@FixMod(a,Const(b)) if !b.isPow2 => Area("DSPs" -> 1)

    case _ => model(lhs)
  }

  /**
    * Returns the area resources for a delay line with the given width (in bits) and length (in cycles)
    * Models delays as registers for short delays, BRAM for long ones
    */
  @stateful def areaOfDelayLine(length: Int, width: Int, par: Int): Area = {
    val nregs = width*length
    val area = RegArea(length, width)*par

    dbg(s"Delay line (w x l): $width x $length (${width*length}) [par = $par]")
    dbg(s"  $area")
//    area
    Area("Regs" -> {length * par})
  }

  @stateful def summarize(area: Area): (Area, String)
}

