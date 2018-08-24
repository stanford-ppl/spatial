package spatial.targets

import argon._
import argon.node._
import forge.tags._
import utils.math.{isPow2,log2}
import utils.implicits.Readable._
import models._

import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._

abstract class AreaModel(target: HardwareTarget) extends SpatialModel[AreaFields](target) {
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

  @stateful def SRAMArea(width: Int, depth: Int, resource: MemoryResource): Area = {
    resource.area(width,depth)
  }

  @stateful def rawBufferControlArea(width: Int, instance: Memory): Area = {
    val nBanks: Int = instance.totalBanks
    val bufferDepth: Int = instance.depth

    val ctrlResourcesPerBank: Area = {
      if (bufferDepth == 1) NoArea
      else MuxArea(bufferDepth, width) + RegArea(bufferDepth, width)
    }

    val ctrlResourcesPerBuffer = ctrlResourcesPerBank * nBanks

    log(s"Control")
    log(s"------------------------")
    log(s"Resources / Bank: $ctrlResourcesPerBank")
    log(s"Buffer resources: $ctrlResourcesPerBuffer")
    ctrlResourcesPerBuffer * bufferDepth
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
  @stateful def areaOfMemory(mem: Sym[_], instance: Memory): Area = {
    val width = wordWidth(mem)
    val dims  = mem.constDims
    areaOfMemory(width, dims, instance)
  }
  @stateful def areaOfMemory(width: Int, dims: Seq[Int], instance: Memory): Area = {
    dbg(s"$instance")
    val memoryArea  = rawMemoryArea(width, dims, instance, instance.resource)
    val controlArea = rawBufferControlArea(width, instance)
    memoryArea + controlArea
  }

  @stateful def areaOfMem(mem: Sym[_]): Area = {
    val instances = mem.duplicates
    instances.map{instance => areaOfMemory(mem, instance) }.fold(NoArea){_+_}
  }
  @stateful def areaOfAccess(access: Sym[_], mem: Sym[_]): Area = {
    val nbits: Int = wordWidth(mem)
    val dims: Seq[Int] = mem.constDims
    val instances = mem.duplicates.zipWithIndex
                                  .filter{case (d,i) => access.dispatches.exists(_._2.contains(i)) }
                                  .map(_._1)

    val addrSize = dims.map{d => log2(d) + (if (isPow2(d)) 1 else 0) }.max
    val multiplier = model("FixMulBig")("b"->18)
    val adder = model("FixAdd")("b"->addrSize)
    val mod   = model("FixMod")("b"->addrSize)
    val flattenCost = dims.indices.map{i => multiplier*i }.fold(NoArea){_+_} + adder*(dims.length - 1)

    // TODO[3]: Cost of address calculation logic
    //val bankAddrCost =

    log(s"Address size: $addrSize")
    log(s"Adder area:   $adder")
    log(s"Mult area:    $multiplier")
    log(s"Mod area:     $mod")
    log(s"Flatten area: $flattenCost")
    //log(s"Banking area: $bankAddrCost")
    flattenCost //+ bankAddrCost
  }

  @stateful def rawRegArea(reg: Sym[_]): Area = model("Reg")("b" -> wordWidth(reg))

  @stateful def areaOfReg(reg: Sym[_]): Area = {
    val instances = reg.duplicates
    instances.map{instance =>
      rawRegArea(reg) + rawBufferControlArea(wordWidth(reg),instance)
    }.fold(NoArea){_+_}
  }

  @stateful def nDups(e: Sym[_]): Int = e.duplicates.length

  @stateful def areaInReduce(e: Sym[_], d: Op[_]): Area = areaOfNode(e, d)

  //  // TODO[5]: Update controller area models
  //  @stateful private def areaOfControl(ctrl: Sym[_]): Area = {
  //    if (ctrl.isInnerControl) NoArea
  //    else if (ctrl.isSeqControl) model("Sequential")("n" -> nStages(ctrl))
  //    else if (ctrl.isOuterPipeControl model("MetaPipe")("n" -> nStages(ctrl))
  //    else if (ctrl.isStreamControl) model("Stream")("n" -> nStages(ctrl))
  //    else NoArea
  //  }

  @stateful def areaOfNode(lhs: Sym[_], rhs: Op[_]): Area = rhs match {
    /** Non-synthesizable nodes */
    case op:Primitive[_] if !op.canAccel => NoArea

    /** Zero area cost */
    case Transient(_)       => NoArea
    case _:DRAMNew[_,_]     => NoArea
    case GetDRAMAddress(_)  => NoArea
    case _:SwitchCase[_]    => NoArea

    // LUTs
    case lut @ LUTNew(dims,_) => model("LUT")("s" -> dims.map(_.toInt).product, "b" -> lut.A.nbits)

    case _:MemAlloc[_,_] if lhs.isLocalMem => areaOfMem(lhs)
    case op:Accessor[_,_]         => areaOfAccess(lhs, op.mem)
    case op:UnrolledAccessor[_,_] => areaOfAccess(lhs, op.mem)
    case op:StatusReader[_]       => NoArea

    case DelayLine(size,data) => areaOfDelayLine(size,nbits(data),1)

//    case tx:DenseTransfer[_,_,_] if tx.isStore =>
//      import spatial.util.memops._
//
//      val p = tx.dram.parsImm.last
//      val w = tx.A.nbits
//      tx.lens.last match {
//        case Exact(c) if (c.toInt*tx.bT.length) % target.burstSize == 0 => model("AlignedStore"+d)("p"->p,"w"->w)
//        case _ => model("UnalignedStore"+d)("p"->p,"w"->w)
//      }
//    case tx: DenseTransfer[_,_,_] if tx.isLoad =>
//      val d = if (tx.lens.length == 1) "1" else "2"
//      val p = boundOf(tx.p).toInt
//      val w = tx.A.nbits
//      tx.lens.last match {
//        case Exact(c) if (c.toInt*tx.bT.length) % target.burstSize == 0 => model("AlignedLoad"+d)("p"->p,"w"->w)
//        case _ => model("UnalignedLoad"+d)("p"->p,"w"->w)
//      }

    case _ => model(lhs)
  }

  /**
    * Returns the area resources for a delay line with the given width (in bits) and length (in cycles)
    * Models delays as registers for short delays, BRAM for long ones
    */
  @stateful def areaOfDelayLine(length: Int, width: Int, par: Int): Area = {
    val nregs = width*length
    // TODO[4]: Should fix this cutoff point to something more real
    val area = if (nregs < 256) RegArea(length, width)*par
    else areaOfMemory(width*par, List(length), Memory.unit(1))

    dbg(s"Delay line (w x l): $width x $length (${width*length}) [par = $par]")
    dbg(s"  $area")
    area
  }

  @stateful def summarize(area: Area): (Area, String)
}

