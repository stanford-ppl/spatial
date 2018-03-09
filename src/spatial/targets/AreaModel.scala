package spatial.targets

import core._
import forge.tags._
import utils.math.{isPow2,log2}
import utils.implicits.Readable._
import models._

import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._

abstract class AreaModel extends SpatialModel[AreaFields] {
  def RegArea(n: Int, bits: Int): Area = model("Reg")("b"->bits, "d"->1) * n
  def MuxArea(n: Int, bits: Int): Area = model("Mux")("b"->bits) * n // TODO: Not sure if this is always right

  final def NoArea: Area = Area.empty[Double]
  final def DSP_CUTOFF: Int = target.DSP_CUTOFF

  @stateful def apply(e: Sym[_], inHwScope: Boolean, inReduce: Boolean): Area = e.op match {
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
    val dims  = constDimsOf(mem)
    memoryBankDepth(width, dims, instance)
  }

  @stateful def memoryBankDepth(width: Int, dims: Seq[Int], instance: Memory): Int = {
    instance.bankDepth(dims)
  }

  @stateful def rawMemoryBankArea(mem: Sym[_], instance: Memory, resource: MemoryResource): Area = {
    val width = wordWidth(mem)
    val dims  = constDimsOf(mem)
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
    val dims  = constDimsOf(mem)
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
    val dims  = constDimsOf(mem)
    areaOfMemory(width, dims, instance)
  }
  @stateful def areaOfMemory(width: Int, dims: Seq[Int], instance: Memory): Area = {
    dbg(s"$instance")
    val memoryArea  = rawMemoryArea(width, dims, instance, instance.resource)
    val controlArea = rawBufferControlArea(width, instance)
    memoryArea + controlArea
  }

  @stateful def areaOfMem(mem: Sym[_]): Area = {
    val instances = duplicatesOf(mem)
    instances.map{instance => areaOfMemory(mem, instance) }.fold(NoArea){_+_}
  }
  @stateful def areaOfAccess(access: Sym[_], mem: Sym[_]): Area = {
    val nbits: Int = wordWidth(mem)
    val dims: Seq[Int] = constDimsOf(mem)
    val instances = duplicatesOf(mem).zipWithIndex
                                     .filter{case (d,i) => dispatchOf(access).exists(_._2.contains(i)) }
                                     .map(_._1)

    val addrSize = dims.map{d => log2(d) + (if (isPow2(d)) 1 else 0) }.max
    val multiplier = model("FixMulBig")("b"->18) //if (addrSize < DSP_CUTOFF) model("FixMulSmall")("b"->addrSize) else model("FixMulBig")("b"->addrSize)
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
    val instances = duplicatesOf(reg)
    instances.map{instance =>
      rawRegArea(reg) + rawBufferControlArea(wordWidth(reg),instance)
    }.fold(NoArea){_+_}
  }

  @stateful def nDups(e: Sym[_]): Int = duplicatesOf(e).length
  @stateful def nStages(e: Sym[_]): Int = childrenOf(e).length

  @stateful def areaInReduce(e: Sym[_], d: Op[_]): Area = areaOfNode(e, d)

  // TODO[5]: Update controller area models
  @stateful private def areaOfControl(ctrl: Sym[_]): Area = {
    if (isInnerControl(ctrl)) NoArea
    //else if (isSeqPipe(ctrl)) model("Sequential")("n" -> nStages(ctrl))
    //else if (isMetaPipe(ctrl)) model("MetaPipe")("n" -> nStages(ctrl))
    //else if (isStreamPipe(ctrl)) model("Stream")("n" -> nStages(ctrl))
    else NoArea
  }

  @stateful def areaOfNode(lhs: Sym[_], rhs: Op[_]): Area = {try { rhs match {
    /** Non-synthesizable nodes */
    case _:PrintIf          => NoArea
    case _:AssertIf         => NoArea
    case _:GenericToText[_] => NoArea
    case _:TextConcat       => NoArea
    case _:VarNew[_]        => NoArea
    case _:VarRead[_]       => NoArea
    case _:VarAssign[_]     => NoArea

    /** Zero area cost */
    case Transient(_)       => NoArea
    case _:DRAMNew[_,_]     => NoArea // No hardware cost
    case GetDRAMAddress(_)  => NoArea // No hardware cost

    /** Memories */
    case op: CounterNew[_] =>
      val par = boundOf(op.par).toInt
      val bits = op.nA.nbits
      model("Counter")("b" -> bits, "p" -> par)

    case CounterChainNew(ctrs) => model("CounterChain")("n" -> ctrs.length)

    // LUTs
    //case lut @ LUTNew(dims,elems) => model("LUT")("s" -> dims.product, "b" -> lut.bT.length) // TODO
    //case _:LUTLoad[_] => NoArea // TODO

    // Streams
    // TODO: Need models for streams
//    case _:StreamInNew[_]       => NoArea
//    case _:StreamOutNew[_]      => NoArea
//    case _:StreamRead[_]        => NoArea
//    case _:ParStreamRead[_]     => NoArea
//    case _:StreamWrite[_]       => NoArea
//    case _:ParStreamWrite[_]    => NoArea
//    case _:BufferedOutWrite[_]  => NoArea

    // TODO: Account for parallelization
    // FIFOs
    case _:FIFONew[_]           => areaOfMem(lhs)
    case _:FIFOEnq[_]           => NoArea
    case _:FIFODeq[_]           => NoArea
    case _:FIFOPeek[_]          => NoArea
    case _:FIFONumel[_]         => NoArea
    case _:FIFOIsAlmostEmpty[_] => NoArea
    case _:FIFOIsAlmostFull[_]  => NoArea
    case _:FIFOIsEmpty[_]       => NoArea
    case _:FIFOIsFull[_]        => NoArea
    case _:FIFOBankedDeq[_]     => NoArea
    case _:FIFOBankedEnq[_]     => NoArea

    // LIFOs
    case _:LIFONew[_]           => areaOfMem(lhs)
    case _:LIFOPush[_]          => NoArea
    case _:LIFOPop[_]           => NoArea
    case _:LIFOPeek[_]          => NoArea
    case _:LIFONumel[_]         => NoArea
    case _:LIFOIsAlmostEmpty[_] => NoArea
    case _:LIFOIsAlmostFull[_]  => NoArea
    case _:LIFOIsEmpty[_]       => NoArea
    case _:LIFOIsFull[_]        => NoArea
    case _:LIFOBankedPush[_]    => NoArea
    case _:LIFOBankedPop[_]     => NoArea

    // SRAMs
    case _:SRAMNew[_,_]          => areaOfMem(lhs)
    case op:SRAMRead[_,_]        => areaOfAccess(lhs, op.mem)
    case op:SRAMWrite[_,_]       => areaOfAccess(lhs, op.mem)
    case op:SRAMBankedRead[_,_]  => areaOfAccess(lhs, op.mem)
    case op:SRAMBankedWrite[_,_] => areaOfAccess(lhs, op.mem)

    // LineBuffer
//    case op:LineBufferNew[_]    => areaOfSRAM(op.bT.length,constDimsOf(lhs),duplicatesOf(lhs))
//    case _:LineBufferEnq[_]     => NoArea
//    case _:ParLineBufferEnq[_]  => NoArea
//    case _:LineBufferLoad[_]    => NoArea
//    case _:ParLineBufferLoad[_] => NoArea

    // Regs
    case _:RegNew[_]   => areaOfReg(lhs)
    case _:RegRead[_]  => NoArea
    case _:RegWrite[_] => NoArea
    case _:RegReset[_] => NoArea

    // ArgIn
    case _:ArgInNew[_]  => areaOfReg(lhs)
    case _:ArgInRead[_] => NoArea
    case _:SetArgIn[_]  => NoArea

    // ArgOut
    case _:ArgOutNew[_]   => areaOfReg(lhs)
    case _:ArgOutWrite[_] => NoArea
    case _:GetArgOut[_]   => NoArea

    // Register File
    case _:RegFileNew[_,_]         => NoArea
    case _:RegFileRead[_,_]        => NoArea
    case _:RegFileWrite[_,_]       => NoArea
    case _:RegFileShiftIn[_,_]     => NoArea
    case _:RegFileBankedRead[_,_]  => NoArea
    case _:RegFileBankedWrite[_,_] => NoArea

    /** Primitives */
    // Bit
    case Not(_)     => model("BitNot")()
    case And(_,_)   => model("BitAnd")()
    case Or(_,_)    => model("BitOr")()
    case Xor(_,_)   => model("BitNeq")()
    case Xnor(_,_)  => model("BitEql")()

    // Fixed point
    case FixNeg(_)   => model("FixNeg")("b" -> nbits(lhs))
    case FixInv(_)   => model("FixInv")("b" -> nbits(lhs))
    case FixAdd(_,_) => model("FixAdd")("b" -> nbits(lhs))
    case FixSub(_,_) => model("FixSub")("b" -> nbits(lhs))
    case FixMul(_,_) => nbits(lhs) match {
      case n if n < DSP_CUTOFF => model("FixMulSmall")("b" -> n)
      case n                   => model("FixMulBig")("b" -> n)
    }
    case FixDiv(_,_) => model("FixDiv")("b" -> nbits(lhs))
    case FixMod(_,_) => model("FixMod")("b" -> nbits(lhs))
    case FixLst(_,_)  => model("FixLt")("b" -> nbits(lhs))
    case FixLeq(_,_) => model("FixLt")("b" -> nbits(lhs))
    case FixNeq(_,_) => model("FixNeq")("b" -> nbits(lhs))
    case FixEql(_,_) => model("FixEql")("b" -> nbits(lhs))
    case FixAnd(_,_) => model("FixAnd")("b" -> nbits(lhs))
    case FixOr(_,_)  => model("FixOr")("b" -> nbits(lhs))
    case FixXor(_,_) => model("FixXor")("b" -> nbits(lhs))
    case FixAbs(_)   => model("FixAbs")("b" -> nbits(lhs))

    // Saturating and/or unbiased math
    case SatAdd(_,_)    => model("SatAdd")("b" -> nbits(lhs))
    case SatSub(_,_)    => model("SatSub")("b" -> nbits(lhs))
    case SatMul(_,_)    => model("SatMul")("b" -> nbits(lhs))
    case SatDiv(_,_)    => model("SatDiv")("b" -> nbits(lhs))
    case UnbMul(_,_)    => model("UnbMul")("b" -> nbits(lhs))
    case UnbDiv(_,_)    => model("UnbDiv")("b" -> nbits(lhs))
    case UnbSatMul(_,_) => model("UnbSatMul")("b" -> nbits(lhs))
    case UnbSatDiv(_,_) => model("UnbSatDiv")("b" -> nbits(lhs))

    // Floating point
    case FltNeg(_)   => lhs.tp match {
      case FloatType()    => model("FloatNeg")()
      case FltPtType(s,e) => model("FltNeg")("s" -> s, "e" -> e)
    }
    case FltAbs(_)   => lhs.tp match {
      case FloatType()    => model("FloatAbs")()
      case FltPtType(s,e) => model("FltAbs")("s" -> s, "e" -> e)
    }
    case FltAdd(_,_) => lhs.tp match {
      case FloatType()    => model("FloatAdd")()
      case FltPtType(s,e) => model("FltAdd")("s" -> s, "e" -> e)
    }
    case FltSub(_,_) => lhs.tp match {
      case FloatType()    => model("FloatSub")()
      case FltPtType(s,e) => model("FltSub")("s" -> s, "e" -> e)
    }
    case FltMul(_,_) => lhs.tp match {
      case FloatType()    => model("FloatMul")()
      case FltPtType(s,e) => model("FltMul")("s" -> s, "e" -> e)
    }
    case FltDiv(_,_) => lhs.tp match {
      case FloatType()    => model("FloatDiv")()
      case FltPtType(s,e) => model("FltDiv")("s" -> s, "e" -> e)
    }
    case FltLst(a,_)  => a.tp match {
      case FloatType()    => model("FloatLt")()
      case FltPtType(s,e) => model("FltLt")("s" -> s, "e" -> e)
    }
    case FltLeq(a,_) => a.tp match {
      case FloatType()    => model("FloatLeq")()
      case FltPtType(s,e) => model("FltLeq")("s" -> s, "e" -> e)
    }
    case FltNeq(a,_) => a.tp match {
      case FloatType()    => model("FloatNeq")()
      case FltPtType(s,e) => model("FltNeq")("s" -> s, "e" -> e)
    }
    case FltEql(a,_) => a.tp match {
      case FloatType()    => model("FloatEql")()
      case FltPtType(s,e) => model("FltEql")("s" -> s, "e" -> e)
    }
    case FltLn(_)   => lhs.tp match {
      case FloatType()    => model("FloatLog")()
      case FltPtType(s,e) => model("FltLog")("s" -> s, "e" -> e)
    }
    case FltExp(_)   => lhs.tp match {
      case FloatType()    => model("FloatExp")()
      case FltPtType(s,e) => model("FltExp")("s" -> s, "e" -> e)
    }
    case FltSqrt(_)  => lhs.tp match {
      case FloatType()    => model("FloatSqrt")()
      case FltPtType(s,e) => model("FltSqrt")("s" -> s, "e" -> e)
    }

    case FltToFix(x,_) => x.tp match {
      case FloatType() => lhs.tp match {
        case FixPtType(_,i,f) => model("FloatToFix")("b"->i,"f"->f)
      }
      case FltPtType(s,e) => lhs.tp match {
        case FixPtType(_,i,f) => model("FltToFix")("s"->s,"e"->e,"b"->i,"f"->f)
      }
    }
    case FixToFlt(x,_) => lhs.tp match {
      case FloatType() => x.tp match {
        case FixPtType(_,i,f) => model("FixToFloat")("b"->i,"f"->f)
      }
      case FltPtType(s,e) => x.tp match {
        case FixPtType(_,i,f) => model("FixToFlt")("s"->s,"e"->e,"b"->i,"f"->f)
      }
    }

    // Other
    case Mux(_,_,_) => model("Mux")("b" -> nbits(lhs))
    case _:FixMin[_,_,_] | _:FixMax[_,_,_] => model("FixLt")("b" -> nbits(lhs)) + model("Mux")("b" -> nbits(lhs))

    case _:FltMin[_,_] | _:FltMax[_,_] => lhs.tp match {
      case FloatType()      => model("FloatLt")() + model("Mux")("b" -> nbits(lhs))
      case DoubleType()     => model("DoubleLt")() + model("Mux")("b" -> nbits(lhs))
      case tp =>
        miss(r"Mux on $tp (rule)")
        NoArea
    }
    //case DelayLine(depth, _)   => areaOfDelayLine(depth,nbits(lhs),1)


    /** Control Structures */
    case _:AccelScope          => areaOfControl(lhs)
    case _:UnitPipe            => areaOfControl(lhs)
    //case _:ParallelPipe        => model("Parallel")("n" -> nStages(lhs))
    case _:OpForeach           => areaOfControl(lhs)
    //case _:OpReduce[_]         => areaOfControl(lhs)
    //case _:OpMemReduce[_,_]    => areaOfControl(lhs)
    //case _:UnrolledForeach     => areaOfControl(lhs)
    //case _:UnrolledReduce[_,_] => areaOfControl(lhs)
    case s:Switch[_] => lhs.tp match {
      case Bits(bt) => model("SwitchMux")("n" -> s.cases.length, "b" -> bt.nbits)
      case _        => model("Switch")("n" -> s.cases.length)
    }
    case _:SwitchCase[_]       => NoArea // Doesn't correspond to anything in hardware


//    case tx:DenseTransfer[_,_] if tx.isStore =>
//      val d = if (tx.lens.length == 1) "1" else "2"
//      val p = boundOf(tx.p).toInt
//      val w = tx.bT.length
//      tx.lens.last match {
//        case Exact(c) if (c.toInt*tx.bT.length) % target.burstSize == 0 => model("AlignedStore"+d)("p"->p,"w"->w)
//        case _ => model("UnalignedStore"+d)("p"->p,"w"->w)
//      }
//    case tx: DenseTransfer[_,_] if tx.isLoad =>
//      val d = if (tx.lens.length == 1) "1" else "2"
//      val p = boundOf(tx.p).toInt
//      val w = tx.bT.length
//      tx.lens.last match {
//        case Exact(c) if (c.toInt*tx.bT.length) % target.burstSize == 0 => model("AlignedLoad"+d)("p"->p,"w"->w)
//        case _ => model("UnalignedLoad"+d)("p"->p,"w"->w)
//      }

    case _ =>
      miss(r"${rhs.getClass} (rule)")
      NoArea
  }}
  catch {case e:Throwable =>
    miss(r"${rhs.getClass}: " + e.getMessage)
    NoArea
  }}

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

  @stateful def summarize(area: Area): Area
}

