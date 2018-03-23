package spatial.codegen.chiselgen

import argon.codegen.FileDependencies
import argon.core._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._
import spatial.utils._
import scala.math._

trait ChiselGenCounter extends ChiselGenSRAM with FileDependencies {
  var streamCtrCopy = List[Bound[_]]()

  // dependencies ::= AlwaysDep("chiselgen", "resources/Counter.chisel")
  def emitCounterChain(lhs: Exp[_], suffix: String = ""): Unit = {
    val Def(CounterChainNew(ctrs)) = lhs
    var isForever = false
    // Temporarily shove ctrl node onto stack so the following is quoted properly
    if (cchainPassMap.contains(lhs)) {controllerStack.push(cchainPassMap(lhs))}
    var maxw = 32 min ctrs.map(cchainWidth(_)).reduce{_*_}
    val counter_data = ctrs.map{ ctr => ctr match {
      case Def(CounterNew(start, end, step, par)) => 
        val w = cchainWidth(ctr)
        (start,end) match { 
          case (Exact(s), Exact(e)) => (src"${s}.FP(true, $w, 0)", src"${e}.FP(true, $w, 0)", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
          case _ => (src"$start", src"$end", src"$step", {src"$par"}.split('.').take(1)(0), src"$w")
        }
      case Def(Forever()) => 
        isForever = true
        ("0.S", "999.S", "1.S", "1", "32") 
    }}
    // TODO: Combine the below with the above, just monkeypatched this to fix issue #233
    val counter_construction = ctrs.map{ ctr => ctr match {
      case Def(CounterNew(start, end, step, par)) => 
        val st = start match {
          case Exact(s) => src"Some($s)"
          case _ => "None"
        }
        val en = end match {
          case Exact(s) => src"Some($s)"
          case _ => "None"
        }
        val ste = step match {
          case Exact(s) => src"Some($s)"
          case _ => "None"
        }
        (st, en, ste, "Some(0)")
      case Def(Forever()) => 
        isForever = true
        ("Some(0)", "Some(999)", "Some(1)", "Some(0)") 
    }}
    if (cchainPassMap.contains(lhs)) {controllerStack.pop()}
    disableSplit = true
    emitGlobalWireMap(src"""${lhs}${suffix}_done""", """Wire(Bool())""")
    emitGlobalWireMap(src"""${lhs}${suffix}_en""", """Wire(Bool())""") // Dangerous but whatever
    emitGlobalWireMap(src"""${lhs}${suffix}_resetter""", """Wire(Bool())""")
    emitGlobalModule(src"""val ${lhs}${suffix}_strides = List(${counter_data.map(_._3)}) // TODO: Safe to get rid of this and connect directly?""")
    emitGlobalModule(src"""val ${lhs}${suffix}_stops = List(${counter_data.map(_._2)}) // TODO: Safe to get rid of this and connect directly?""")
    emitGlobalModule(src"""val ${lhs}${suffix}_starts = List(${counter_data.map{_._1}}) """)
    emitGlobalModule(src"""val ${lhs}${suffix} = Module(new templates.Counter(List(${counter_data.map(_._4)}), 
  List(${counter_construction.map(_._1)}), List(${counter_construction.map(_._2)}), List(${counter_construction.map(_._3)}), List(${counter_construction.map(_._4)}), List(${counter_data.map(_._5)}))) // Par of 0 creates forever counter""")

    emit(src"""${lhs}${suffix}.io.input.stops.zip(${lhs}${suffix}_stops).foreach { case (port,stop) => port := stop.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.strides.zip(${lhs}${suffix}_strides).foreach { case (port,stride) => port := stride.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.starts.zip(${lhs}${suffix}_starts).foreach { case (port,start) => port := start.r.asSInt }""")
    emit(src"""${lhs}${suffix}.io.input.gaps.foreach { gap => gap := 0.S }""")
    emit(src"""${lhs}${suffix}.io.input.saturate := false.B""")
    emit(src"""${lhs}${suffix}.io.input.enable := ${swap(src"${lhs}${suffix}", En)}""")
    emit(src"""${swap(src"${lhs}${suffix}", Done)} := ${lhs}${suffix}.io.output.done""")
    emit(src"""${lhs}${suffix}.io.input.reset := ${swap(src"${lhs}${suffix}", Resetter)}""")
    if (suffix != "") {
      emit(src"""${lhs}${suffix}.io.input.isStream := true.B""")
    } else {
      emit(src"""${lhs}${suffix}.io.input.isStream := false.B""")      
    }
    emit(src"""val ${lhs}${suffix}_maxed = ${lhs}${suffix}.io.output.saturated""")
    ctrs.zipWithIndex.foreach { case (c, i) =>
      val x = c match {
        case Def(CounterNew(_,_,_,Literal(p))) => p
        case Def(Forever()) => 1
      }
      if (suffix == "") {emitGlobalWireMap(s"""${quote(c)}""", src"""Wire(Vec($x, SInt(${counter_data(i)._5}.W)))""")}
      else {emitGlobalWire(s"""val ${quote(c)}${suffix} = (0 until $x).map{ j => Wire(SInt(${counter_data(i)._5}.W)) }""")}
      emit(s"""(0 until $x).map{ j => ${quote(c)}${suffix}(j) := ${quote(lhs)}${suffix}.io.output.counts($i + j) }""")
    }

    disableSplit = false
  }

  private def getCtrSuffix(head: Exp[_]): String = {
    if (parentOf(head).isDefined) {
      if (styleOf(parentOf(head).get) == StreamPipe) {src"_copy${head}"} else {getCtrSuffix(parentOf(head).get)}  
    } else {
      "" // TODO: Should this actually throw error??
    }
    
  }

  private def getValidSuffix(head: Exp[_], candidates: Seq[Exp[_]]): String = {
    // Specifically check if head == parent of candidates and do not add suffix if so
    if (!candidates.isEmpty && parentOf(candidates.head).get == head) {
      ""
    } else {
      if (candidates.contains(head)) {
        val id = candidates.toList.indexOf(head)
        if (id > 0) src"_chain_read_${id}" else ""
      } else {
        if (parentOf(head).isDefined) {
          getValidSuffix(parentOf(head).get, candidates)
        } else {
          "" // TODO: Should this actually throw error??
        }
      }
    }
  }

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: CounterNew)      => s"${s}_ctr"
    case Def(_: CounterChainNew) => s"${s}_ctrchain"
    case _ => super.name(s)
  }

  override protected def quote(e: Exp[_]): String = e match {
    // FIXME: Unclear precedence with the quote rule for Bound in ChiselGenSRAM
    case b: Bound[_] =>
      if (streamCtrCopy.contains(b)) {
        if (validPassMap.contains((e, getCtrSuffix(controllerStack.head)) )) {
          swap(super.quote(e) + getCtrSuffix(controllerStack.head) + getValidSuffix(controllerStack.head, validPassMap(e, getCtrSuffix(controllerStack.head))), Blank)
        } else {
          swap(super.quote(e) + getCtrSuffix(controllerStack.head), Blank)
        }
      } else {
        if (validPassMap.contains((e, "") )) {
          swap(super.quote(e) + getValidSuffix(controllerStack.head, validPassMap(e, "")), Blank)
        } else {
          super.quote(e)
        }
      }
    case _ => super.quote(e)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case CounterType      => src"Counter"
    case CounterChainType => src"Array[Counter]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => 
      emit(s"// $lhs = ($start to $end by $step par $par")
    case CounterChainNew(ctrs) => 
      val user = usersOf(lhs).head._1
      if (styleOf(user) != StreamPipe) emitCounterChain(lhs)
    case Forever() => 
      emit("// $lhs = Forever")

    case _ => super.emitNode(lhs, rhs)
  }

}
