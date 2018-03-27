package spatial.codegen.naming

import argon._
import argon.codegen._

import spatial.lang._
import spatial.node._

import scala.collection.mutable

trait NamedCodegen extends Codegen {
  var compressorMap = mutable.HashMap[String, (String,Int)]()
  var retimeList = mutable.ListBuffer[String]()
  val pipeRtMap = mutable.HashMap[(String,Int), String]()

  /** Map for tracking defs of nodes. If they get redeffed anywhere, we map it to a suffix */
  var alphaconv = mutable.HashMap[String, String]()

  override def named(s: Sym[_], id: Int): String = s.op match {
    case Some(rhs) => rhs match {
      case _: AccelScope       => s"${s}_RootController${s._name}"
      case _: UnitPipe         => s"${s}_UnitPipe${s._name}"
      case _: UnrolledForeach  => s"${s}_Foreach${s._name}"
      case _: UnrolledReduce   => s"${s}_Reduce${s._name}"
      case _: Switch[_]        => s"${s}_Switch${s._name}"
      case _: SwitchCase[_]    => s"${s}_SwitchCase${s._name}"
      case _: StateMachine[_]  => s"${s}_FSM${s._name}"
      case _: CounterNew[_]    => s"${s}_ctr"
      case _: CounterChainNew  => s"${s}_ctrchain"

      case DelayLine(size, data) => data match {
        case Const(_) => src"$data"
        case _ => wireMap(src"${data}_D$size" + alphaconv.getOrElse(src"${data}_D$size", ""))
      }

      case DRAMNew(_,_) => s"${s}_${s.nameOr("dram")}"
      case ArgInNew(_)  => s"${s}_${s.nameOr("argIn")}"
      case ArgOutNew(_) => s"${s}_${s.nameOr("argOut")}"
      case RegNew(_)    => s"${s}_${s.nameOr("reg")}"
      case RegFileNew(_,_) => s"${s}_${s.nameOr("regfile")}"
      case FIFONew(_)   => s"${s}_${s.nameOr("fifo")}"
      case LIFONew(_)   => s"${s}_${s.nameOr("lifo")}"
      case SRAMNew(_)   => s"${s}_${s.nameOr("sram")}"
      case LUTNew(_,_)  => s"${s}_${s.nameOr("lut")}"

      case SetArgIn(reg,_)      => s"${s}_${s.nameOr(src"set_$reg")}"
      case ArgInRead(reg)       => s"${s}_${s.nameOr(src"rd_$reg")}"
      case ArgOutWrite(reg,_,_) => s"${s}_${s.nameOr(src"wr_$reg")}"
      case GetArgOut(reg)       => s"${s}_${s.nameOr(src"get_$reg")}"

      case RegRead(reg)      => s"${s}_${s.nameOr(src"rd_$reg")}"
      case RegWrite(reg,_,_) => s"${s}_${s.nameOr(src"wr_$reg")}"

      case _:SRAMBankedRead[_,_]  => s"${s}_${s.nameOr("rd")}"
      case _:SRAMBankedWrite[_,_] => s"${s}_${s.nameOr("wr")}"

      case FIFOBankedEnq(fifo,_,_)   => s"${s}_${s.nameOr(src"enq_$fifo")}"
      case FIFOBankedDeq(fifo,_)     => s"${s}_${s.nameOr(src"deq_$fifo")}"
      case FIFOIsEmpty(fifo,_)       => s"${s}_${s.nameOr(src"isEmpty_$fifo")}"
      case FIFOIsFull(fifo,_)        => s"${s}_${s.nameOr(src"isFull_$fifo")}"
      case FIFOIsAlmostEmpty(fifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_$fifo")}"
      case FIFOIsAlmostFull(fifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_$fifo")}"
      case FIFONumel(fifo,_)         => s"${s}_${s.nameOr(src"numel_$fifo")}"

      case LIFOBankedPush(lifo,_,_)  => s"${s}_${s.nameOr(src"push_$lifo")}"
      case LIFOBankedPop(lifo,_)     => s"${s}_${s.nameOr(src"pop_$lifo")}"
      case LIFOIsEmpty(lifo,_)       => s"${s}_${s.nameOr(src"isEmpty_$lifo")}"
      case LIFOIsFull(lifo,_)        => s"${s}_${s.nameOr(src"isFull_$lifo")}"
      case LIFOIsAlmostEmpty(lifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_$lifo")}"
      case LIFOIsAlmostFull(lifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_$lifo")}"
      case LIFONumel(lifo,_)         => s"${s}_${s.nameOr(src"numel_$lifo")}"

      case VecAlloc(_)           => s"${s}_vec"
      case VecApply(_,i)         => s"${s}_elem_$i"
      case VecSlice(_,start,end) => s"${s}_slice_${start}_to_$end"
      case _: SimpleStruct[_]    => s"${s}_tuple"
      case _: FieldApply[_,_]    => s"${s}_apply"

      case FixRandom(_) => s"${s}_fixrnd"
      case FixNeg(x)    => s"${s}_${s.nameOr(s"neg$x")}"
      case FixAdd(_,_)  => s"${s}_${s.nameOr("sum")}"
      case FixSub(_,_)  => s"${s}_${s.nameOr("sub")}"
      case FixDiv(_,_)  => s"${s}_${s.nameOr("div")}"
      case FixMul(_,_)  => s"${s}_${s.nameOr("mul")}"

      case _ => super.named(s,id)
    }
    case _ => super.named(s,id)
  }

  final protected def wireMap(x: String): String = {
    if (compressorMap.contains(x)) {
      src"${listHandle(compressorMap(x)._1)}(${compressorMap(x)._2})"
    }
    else {
      x
    }
  }

  final protected def listHandle(rhs: String): String = {
    val vec = if (rhs.contains("Vec")) {
      val width_extractor = "Wire\\([ ]*Vec\\(([0-9]+)[ ]*,.*".r
      val width_extractor(vw) = rhs
      s"vec${vw}_"
    } else {""}
      if (rhs.contains("Bool()")) {
      s"${vec}b"
    } else if (rhs.contains("SRFF()")) {
      s"${vec}srff"
    } else if (rhs.contains("UInt(")) {
      val extractor = ".*UInt\\(([0-9]+).W\\).*".r
      val extractor(width) = rhs
      s"${vec}u${width}"
    } else if (rhs.contains("SInt(")) {
      val extractor = ".*SInt\\(([0-9]+).W\\).*".r
      val extractor(width) = rhs
      s"${vec}s${width}"
    } else if (rhs.contains(" FixedPoint(")) {
      val extractor = ".*FixedPoint\\([ ]*(.*)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(s,i,f) = rhs
      val ss = if (s.contains("rue")) "s" else "u"
      s"${vec}fp${ss}${i}_${f}"
    } else if (rhs.contains(" FloatingPoint(")) {
      val extractor = ".*FloatingPoint\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(m,e) = rhs
      s"${vec}flt${m}_${e}"
    } else if (rhs.contains(" NBufFF(") && !rhs.contains("numWriters")) {
      val extractor = ".*NBufFF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}nbufff${d}_${w}"
    } else if (rhs.contains(" NBufFF(") && rhs.contains("numWriters")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*,[ ]*numWriters[ ]*=[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w,n) = rhs
      s"${vec}ff${d}_${w}_${n}wr"
    } else if (rhs.contains(" templates.FF(")) {
      val extractor = ".*FF\\([ ]*([0-9]+)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(d,w) = rhs
      s"${vec}ff${d}_${w}"
    } else if (rhs.contains(" R_Info(")) {
      val extractor = ".*R_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*\\).*".r
      val extractor(n,dims) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdr${n}_${d}"
    } else if (rhs.contains(" W_Info(")) {
      val extractor = ".*W_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdw${n}_${d}_${w}"
    } else if (rhs.contains(" RegR_Info(")) {
      val extractor = ".*RegR_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*\\).*".r
      val extractor(n,dims) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}ri${n}_${d}"
    } else if (rhs.contains(" RegW_Info(")) {
      val extractor = ".*RegW_Info\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9,]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}wi${n}_${d}_${w}"
    } else if (rhs.contains(" multidimRegW(")) {
      val extractor = ".*multidimRegW\\([ ]*([0-9]+)[ ]*,[ ]*List\\(([0-9, ]+)\\)[ ]*,[ ]*([0-9]+)[ ]*\\).*".r
      val extractor(n,dims,w) = rhs
      val d = dims.replace(" ", "").replace(",","_")
      s"${vec}mdrw${n}_${d}_${w}"
    } else if (rhs.contains(" Seqpipe(")) {
      val extractor = ".*Seqpipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}seq${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Metapipe(")) {
      val extractor = ".*Metapipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}meta${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Innerpipe(")) {
      val extractor = ".*Innerpipe\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}inner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streaminner(")) {
      val extractor = ".*Streaminner\\([ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(strm,ctrd,stw,static,isRed) = rhs
      val st = strm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strinner${st}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Parallel(")) {
      val extractor = ".*Parallel\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}parallel${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains(" Streampipe(")) {
      val extractor = ".*Streampipe\\([ ]*([0-9]+)[ ]*,[ ]*isFSM[ ]*=[ ]*([falsetrue]+)[ ]*,[ ]*ctrDepth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*stateWidth[ ]*=[ ]*([0-9]+)[ ]*,[ ]*staticNiter[ ]*=[ ]*([falsetrue]+),[ ]*isReduce[ ]*=[ ]*([falsetrue]+)\\).*".r
      val extractor(stages,fsm,ctrd,stw,static,isRed) = rhs
      val f = fsm.replace("false", "f").replace("true", "t")
      val s = static.replace("false", "f").replace("true", "t")
      val ir = isRed.replace("false", "f").replace("true", "t")
      s"${vec}strmpp${stages}_${f}_${ctrd}_${stw}_${s}_${ir}"
    } else if (rhs.contains("_retime")) {
      "rt"
    } else {
      throw new Exception(s"Cannot compress ${rhs}!")
    }
  }

}
