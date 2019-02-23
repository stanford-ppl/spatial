package spatial.model

import argon._
import argon.codegen.Codegen
import spatial.node._
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.util.modeling._

trait ControlModels { this: RuntimeModelGenerator =>
  import spatial.dsl._

}

case class RuntimeModelGenerator(IR: State, version: String) extends Codegen with ControlModels {
  override val ext: String = ".scala"
  override val lang: String = "model"
  override val entryFile: String = s"model_${version}.scala"

  var inCycle: Boolean = false
  var undefinedSyms: Set[Sym[_]] = Set.empty



  def getCtx(lhs: Sym[_]): String = s"${lhs.ctx.content.map(_.trim).getOrElse("(Inserted by compiler)")}"


  override def quote(s: Sym[_]): String = s.rhs match {
    case Def.TypeRef  => super.quote(s)
    case Def.Const(x) => s"$x"
    case Def.Bound(_) => super.quote(s)
    case Def.Error(_,_) => super.quote(s)
    case Def.Node(id,op) =>
      if (s.isInstanceOf[Fix[_,_,_]]) undefinedSyms += s
      super.quote(s)
    case Def.Param(id, _) =>
      if (s.isInstanceOf[Fix[_,_,_]]) undefinedSyms += s
      s"p$id"
  }

  def ctrHead(lhs: String, cchain: CounterChain): Unit = {
    cchain.counters.zipWithIndex.foreach { case (counter, i) =>
      emit(src"// Parallelization of counter #$i")
      emit(src"${lhs}_counter${i}_P = ${counter.ctrPar}")
    }
  }

  def nIters(lhs: String, cchain: CounterChain, N: String, P: String): Unit = {
    cchain.counters.zipWithIndex.foreach { case (counter, i) =>
      emit(src"// Number of iterations of counter #$i")
      emit(src"${lhs}_counter${i}_N = ceil( ceil((${counter.start} - ${counter.end}) / ${counter.step}) / ${lhs}_counter${i}_P)")
    }
    if (cchain.counters.isEmpty) {
      emit(src"$N = 1.0")
      emit(src"$P = 1.0")
    }
    else {
      emit(src"$P = ${cchain.counters.indices.map { i => src"${lhs}_counter${i}_N" }.mkString(" * ")}")
      emit(src"$name = ${cchain.counters.indices.map { i => src"${lhs}_counter${i}_P" }.mkString(" * ")}")
    }
  }

  // def memPars(lhs: String, mem: Sym[_]): Seq[String] = mem match {
  //   case Op(alias: MemDenseAlias[_, _, _]) => alias.ranges.zipWithIndex.map { case (rng, d) =>
  //     val series: Series[Idx] = rng.last
  //     series.par match {case Final(s) => s"$s"; case Expect(s) => s"$s"; case _ => s"$par"}
  //   }
  //   case Op(mem: MemAlloc[_, _]) => (1 to mem.rank.length).map { d =>
  //     "1"
  //   }
  //   case _ => throw new Exception(s"Unknown memory type for symbol $mem")
  // }

  def memSizes(lhs: String, mem: Sym[_]): Unit = {
    val rank = mem match {
      case Op(alias: MemDenseAlias[_, _, _]) =>
        alias.ranges.zipWithIndex.foreach { case (rng, d) =>
          val series: Series[Idx] = rng.last
          emit(src"${lhs}_dim$d = ( (${series.end} - ${series.start} + ${series.step} - 1)/${series.step})")
        }
        alias.rawRank.length
      case Op(alloc: MemAlloc[_, _]) =>
        (1 to alloc.rank.length).foreach{d =>
          emit(src"# Parallelization in dimension #d")
          emit(src"${lhs}_dim$d = ${alloc.dims(d)}")
        }
        alloc.rank.length
      case _ => throw new Exception(s"Unknown memory type for symbol $mem")
    }
    emit(src"${lhs}_dims = [${(1 to rank).map{d => src"${lhs}_dim$d" }.mkString(",")}]")
    emit(src"${lhs}_pa modelrs = [${(1 to rank).map{d => src"${lhs}_P$d" }.mkString(",")}]")
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    emit(src"package model")
    emit(src"import models.Runtime._")
    emit(src"")
    open(src"object AppRuntimeModel_${version} extends App {")
      open(src"def build_model(): ControllerModel = {")
        visitBlock(block)
      close("}")
      emit("")
      open("override def main(args: Array[String]): Unit = {")
        emit(s"""begin(sys.env("PWD") + "/results_$version")""")
        emit("""if (args.size >= 1 && (args(0) == "noninteractive" || args(0) == "ni")) {""")
        emit("""    interactive = false""")
        emit("""    cliParams = args.drop(1).map(_.toInt)""")
        emit("""    emit(s"Noninteractive Args: ${cliParams.mkString(" ")}") """)
        emit("""}""")
        emit("val root = build_model()")
        emit("root.initializeAskMap(AskMap.map)")
        emit("root.loadPreviousAskMap(PreviousAskMap.map) // Load previous run's askmap")
        emit(s"""emit(s"[$version] Structure for app ${config.name}")""")
        emit("root.printStructure()")
        emit("root.execute()")
        emit(s"""emit(s"[$version] Runtime results for app ${config.name}")""")
        emit("""root.printResults()""")
        emit("""root.storeAskMap(sys.env("PWD") + "/model/PreviousAskMap.scala") // Store this run's askmap""")
        emit(s"""emit(s"[$version] Total Cycles for App ${config.name}: $${root.totalCycles()}")""")
        emit("end()")
      close("}")
    close("}")

    withGen(out, "InitAskMap.scala"){
      emit(src"package model")
      open(src"object AskMap {")
        emit(src"val map = scala.collection.mutable.Map[Int,Int]()")
        undefinedSyms.foreach{sym =>
          val value = sym match{case Param(c) => s"$c"; case _ => "100" } // TODO: Choose some default value. Should warn?
          emit(src"map += ($sym -> $value)")
        }
      close("}")
    }
    withGen(out, "PreviousAskMap.scala"){
      emit("package model")
      open("object PreviousAskMap{val map = scala.collection.mutable.Map[Int,Int]()}")
    }
  }

  protected def bitWidth(tp: Type[_]): Int = tp match {
    case Bits(bT) => bT.nbits
    case _ => -1
  }

  protected def createCtrObject(lhs: Sym[_], start: Sym[_], stop: Sym[_], step: Sym[_], par: I32, forever: Boolean, sfx: String = ""): Unit = {
    val w = try {bitWidth(lhs.tp.typeArgs.head)} catch {case e: Exception => 32}
    val ctx = s"""Ctx("${lhs}$sfx", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
    val strt = start match {
                 case _ if forever => "Left(0)"
                 case Final(s) => src"Left($s)"
                 case Expect(s) => src"Left($s)"
                 case Param(s) => undefinedSyms += start; src"""Right(Ask(${start.hashCode}, "ctr start", $ctx))"""
                 case _ => src"""Right(Ask(${start.hashCode}, "ctr start", $ctx))"""
                }
    val question = 
      if (sfx.contains("_ctr")) s"""length of dim #${sfx.replace("_ctr","")}""" 
      else if (sfx.contains("_fsm")) s"expected # iters for fsm"
      else "ctr stop"
    val stp = stop match {
                 case _ if forever => "Left(Some(5))"
                 case Final(s) => src"Left($s)"
                 case Expect(s) => src"Left($s)"
                 case Param(s) => undefinedSyms += stop; src"""Right(Ask(${start.hashCode}, "$question", $ctx))"""
                 case _ => src"""Right(Ask(${stop.hashCode}, "$question", $ctx))"""
                }
    val ste = step match {
                 case _ if forever => "Left(Some(0))"
                 case Final(s) => src"Left($s)"
                 case Expect(s) => src"Left($s)"
                 case Param(s) => undefinedSyms += step; src"""Right(Ask(${start.hashCode}, "ctr step", $ctx))"""
                 case _ => src"""Right(Ask(${step.hashCode}, "ctr step", $ctx))"""
                }
    val p = par match {case Final(s) => s"$s"; case Expect(s) => s"$s"; case _ => s"$par"}
    emit(src"val ${lhs}$sfx = CtrModel($strt, $stp, $ste, $p)")
  }

  protected def createCChainObject(lhs: Sym[_], ctrs: Seq[Sym[_]]): Unit = {
    var isForever = lhs.isForever
    val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${ctrs.map{stm}}")"""
    emit(src"""val $lhs = CChainModel(List[CtrModel](${ctrs.map(quote).mkString(",")}), $ctx)""")
  }

  override def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    // case AccelScope(block) if lhs.isInnerControl =>
    //   val body = latencyOfPipe(block).toInt
    //   val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${lhs.ctx.content.getOrElse("???")}", "${stm(lhs)}")"""
    //   emit(src"val $lhs = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, 1, $body, 1, $ctx)")
    //   emit(src"${lhs}")

    case CounterNew(start,end,step,par) => createCtrObject(lhs, start, end, step, par, false) //createCtr(lhs,start,end,step,par)
    case CounterChainNew(ctrs) => createCChainObject(lhs,ctrs)

    case AccelScope(block) =>
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      emit(src"val $lhs = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, CChainModel(Seq()), ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(block)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}
      emit(src"${lhs}")

    case OpForeach(ens,cchain,block,_,_) if (lhs.getLoweredTransfer.isDefined) =>
      // TODO: Include last level counter?
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      createCtrObject(lhs, Bits[I32].zero,lhs.loweredTransferSize._1,Bits[I32].one,lhs.loweredTransferSize._2, false, s"_ctrlast")
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.loweredTransfer.toString}, List($cchain, CChainModel(List(${lhs}_ctrlast))), ${lat.toInt}, ${ii.toInt}, $ctx)")

    case OpForeach(ens,cchain,block,_,_) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, $cchain, ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(block)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case UnrolledForeach(ens,cchain,func,iters,valids,stopWhen) if (lhs.getLoweredTransfer.isDefined) =>
      // TODO: Include last level counter?
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      createCtrObject(lhs, Bits[I32].zero,lhs.loweredTransferSize._1,Bits[I32].one,lhs.loweredTransferSize._2, false, s"_ctrlast")
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.loweredTransfer.toString}, List($cchain, CChainModel(List(${lhs}_ctrlast))), ${lat.toInt}, ${ii.toInt}, $ctx)")

    case UnrolledForeach(ens,cchain,func,iters,valids,stopWhen) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, $cchain, ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(func)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case ParallelPipe(_,block) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, CChainModel(Seq()), 0, 0, $ctx)")
      visitBlock(block)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}


    case UnitPipe(_, block) if (lhs.getLoweredTransfer.isDefined) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      createCtrObject(lhs, Bits[I32].zero,lhs.loweredTransferSize._1,Bits[I32].one,lhs.loweredTransferSize._2, false, s"_ctrlast")
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.loweredTransfer.toString}, List(CChainModel(Seq()), CChainModel(Seq(${lhs}_ctrlast))), ${lat.toInt}, ${ii.toInt}, $ctx)")

    case UnitPipe(_, block) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, CChainModel(Seq()), ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(block)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case OpReduce(ens, cchain, _, map, load, reduce, store, _,_,_,_) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, $cchain, ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(map)
      visitBlock(load)
      visitBlock(reduce)
      visitBlock(store)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case UnrolledReduce(ens,cchain,func,iters,valids,stopWhen) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, $cchain, ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(func)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case tx:DenseTransfer[_,_,_] =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""

      val tp = if (tx.isStore) "DenseStore" else "DenseLoad"
      val pars = tx.pars.map(_.asInstanceOf[Sym[_]]).map(_ match {case Final(s) => s.toInt; case Expect(s) => s.toInt; case _ => 1})
      val steps = tx.ctrSteps.map(_.asInstanceOf[Sym[_]])
      val lens = tx.lens.map(_.asInstanceOf[Sym[_]])

      // Generate ctrs
      List.tabulate(pars.size){i => createCtrObject(lhs, Bits[I32].zero,lens(i),steps(i),pars(i), false, s"_ctr$i")}
      emit(src"""val ${lhs}_cchain = List(CChainModel(List[CtrModel](${pars.dropRight(1).zipWithIndex.map{case (_,i) => s"${lhs}_ctr$i"}.mkString(",")}), $ctx), CChainModel(List(${lhs}_ctr${pars.size-1}), $ctx))""")

      val lat = 0.0
      val ii = 0.0
      emit(src"val ${lhs} = new ControllerModel(OuterControl, ${tp}, ${lhs}_cchain, ${lat.toInt}, ${ii.toInt}, $ctx)")


    case StateMachine(ens, start, notDone, action, nextState) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      createCtrObject(lhs, Bits[I32].zero,lhs,Bits[I32].one,1, false, s"_fsm")
      emit(src"""val ${lhs}_cchain = CChainModel(List[CtrModel](${lhs}_fsm), $ctx)""")
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, ${lhs}_cchain, ${lat.toInt} + 2, ${ii.toInt} + 2, $ctx)") // TODO: Add 2 because it seems to be invisible latency?
      visitBlock(notDone)
      visitBlock(action)
      visitBlock(nextState)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case OpMemReduce(ens, cchainMap, cchainRed, _, map, loadRes, loadAcc, reduce, storeAcc, _, _, _, _, _) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, List($cchainMap, $cchainRed), ${lat.toInt}, ${ii.toInt}, $ctx)")
      visitBlock(map)
      visitBlock(loadRes)
      visitBlock(loadAcc)
      visitBlock(storeAcc)
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}


    case Switch(selects, body) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      visitBlock(body)
      emit(src"val ${lhs} = new ControllerModel(OuterControl, ${lhs.rawSchedule.toString}, CChainModel(List()), ${lat.toInt} + 2, ${ii.toInt} + 2, $ctx)") // TODO: Add 2 because it seems to be invisible latency?
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case SwitchCase(body) =>
      val ctx = s"""Ctx("$lhs", "${lhs.ctx.line}", "${getCtx(lhs).replace("\"","'")}", "${stm(lhs)}")"""
      val lat = if (lhs.isInnerControl) scrubNoise(lhs.bodyLatency.sum) else 0.0
      val ii = if (lhs.II <= 1 | lhs.isOuterControl) 1.0 else scrubNoise(lhs.II)
      visitBlock(body)
      emit(src"val ${lhs} = new ControllerModel(${lhs.level.toString}, ${lhs.rawSchedule.toString}, CChainModel(List()), ${lat.toInt} + 2, ${ii.toInt} + 2, $ctx)") // TODO: Add 2 because it seems to be invisible latency?
      lhs.children.filter(_.s.get != lhs).foreach{x => emit(src"$lhs.registerChild(${x.s.get})")}

    case _ => lhs.blocks.foreach{block => visitBlock(block) }
  }

}