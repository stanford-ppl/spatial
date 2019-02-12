package spatial.model

import argon._
import argon.codegen.Codegen
import spatial.node._
import spatial.lang._
import spatial.metadata.control._
import spatial.util.modeling._

trait ControlModels { this: PythonModelGenerator =>
  import spatial.dsl._

  val common = src"""
       |def prod(xs):
       |  return reduce(operator.mul, xs, 1)
       |
       |## Control overhead
       |sync = 1
       |
       |## Control schemes
       |SEQUENTIAL = 0
       |INNER_PIPELINE = 1
       |OUTER_PIPELINE = 2
       |STREAMING = 3
       |PARALLEL = 4
       |
       |## Controller models
       |
       |# Calculates the runtime of an outer controller in cycles
       |#   N  - number of iterations
       |#   II - loop initiation interval (or 1, if not a loop)
       |#   stages - the runtimes of the inner stages
       |#   schedule - the schedule of the controller (e.g. SEQUENTIAL, STREAMING)
       |def control_model(N, II, stages, schedule):
       |  if schedule == SEQUENTIAL:
       |    return sum(stages) * N + sync
       |  elif schedule == OUTER_PIPELINE:
       |    return max(stages) + (N - 1)*II + sum(stages) + sync
       |  elif schedule == STREAMING:
       |    return max(stages) + sync
       |  elif schedule == PARALLEL:
       |    return max(stages) + sync
       |  else:
       |    return -1.0
       |
       |# Calculates the runtime of an inner controller in cycles
       |#   N - number of loop iterations
       |#   II - loop initiation interval (or 1, if not a loop)
       |#   L - the pipeline latency of the controller, in cycles
       |#   schedule - the controller's execution schedule (SEQUENTIAL or INNER_PIPELINE)
       |def inner_pipeline_runtime_model(N, II, L, schedule):
       |  if schedule == SEQUENTIAL:
       |    return N*L + sync
       |  elif schedule == INNER_PIPELINE:
       |    return L + (N - 1)*II + sync
       |  else:
       |    return -1.0
       |
       |# Estimates the runtime of a dense memory store in cycles.
       |# This is based on a parameteric function derived from bandwidth tests.
       |#   dims - the dimensions of the transfer
       |#   pars - the parallelization factors for the transfer
       |#   word_width - the size of each transferred word, in bits
       |def memory_store_runtime_model(dims, pars, word_width):
       |  cmd = dims[-1]
       |  cmdP = pars[-1]
       |  iters = prod(dims[:-1])
       |  itersP = prod(pars[:-1])
       |
       |  baseCycles = (cmd / cmdP) * (word_width / 32.0)
       |  oFactor = 0.02*p - 0.019
       |  smallOverhead = 0.0175
       |  if (p < 8): smallOverhead = 0.0
       |  if (p < 8):
       |    overhead = oFactor * p + (1 - 8*oFactor) + smallOverhead*8
       |  else:
       |    overhead = 1.0 + smallOverhead*p
       |
       |  return ceil(baseCycles*overhead) * ceil(iters/itersP)
       |
       |# Estimates the runtime of a dense memory load in cycles.
       |# This is based on a parameteric function derived from bandwidth tests.
       |#   dims - the dimensions of the transfer
       |#   pars - the parallelization factors for the transfer
       |#   word_width - the size of each transferred word, in bits
       |def memory_load_runtime_model(dims, pars, word_width):
       |  cmd = dims[-1]
       |  cmdP = pars[-1]
       |  iters = prod(dims[:-1])
       |  itersP = prod(pars[:-1])
       |
       |  cols = cmd
       |  if (cols < 96): cols = 96
       |
       |  overhead12 = 0.125
       |  if cols <= 96: overhead12 = 0.145
       |  else if cols <= 192: overhead12 = 0.137
       |  overhead = 0.926*log(itersP)*overhead12
       |
       |  return ceil((1+overhead)*(110 +(53 + cols)) * ceil(iters / itersP)
       |
     """.stripMargin
}

case class PythonModelGenerator(IR: State) extends Codegen with ControlModels {
  override val ext: String = ".py"
  override val lang: String = "python"
  override val entryFile: String = "model.py"

  var inCycle: Boolean = false
  var undefinedSyms: Set[Sym[_]] = Set.empty

  def cmntHead(): Unit = emit("#" * 80)

  def cmntFoot(): Unit = {
    emit(src"")
    cmntHead()
    emit(src"")
    emit(src"")
  }

  def cmnt(line: String): Unit = {
    val paddedLine = line + " " * Math.max(0, line.length - 72)
    emit(s"### $paddedLine ###")
  }

  def cmntCtx(lhs: Sym[_]): Unit = cmnt(src"${lhs.ctx}: ${lhs.ctx.content.map(_.trim).getOrElse("(Inserted by compiler)")}")

  def schedule(lhs: Sym[_]): String = {
    if (lhs.isStreamControl) "STREAMING"
    else if (lhs.isParallel) "PARALLEL"
    else if (lhs.isSeqControl) "SEQUENTIAL"
    else if (lhs.isInnerControl) "INNER_PIPELINE"
    else if (lhs.isOuterPipeControl) "OUTER_PIPELINE"
    else throw new Exception("Unknown control scheme")
  }

  override def quote(s: Sym[_]): String = s.rhs match {
    case Def.TypeRef  => super.quote(s)
    case Def.Const(_) => super.quote(s)
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
      emit(src"# Parallelization of counter #$i")
      emit(src"${lhs}_counter${i}_P = ${counter.ctrPar}")
    }
  }

  def nIters(lhs: String, cchain: CounterChain, N: String, P: String): Unit = {
    cchain.counters.zipWithIndex.foreach { case (counter, i) =>
      emit(src"# Number of iterations of counter #$i")
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

  def memPars(lhs: String, mem: Sym[_]): Unit = mem match {
    case Op(alias: MemDenseAlias[_, _, _]) => alias.ranges.zipWithIndex.foreach { case (rng, d) =>
      val series: Series[Idx] = rng.last
      emit(src"# Parallelization in dimension #$d")
      emit(src"${lhs}_P$d = ${series.par}")
    }
    case Op(mem: MemAlloc[_, _]) => (1 to mem.rank.length).foreach { d =>
      emit(src"# Parallelization in dimension #d")
      emit(src"${lhs}_P$d = 1")
    }
    case _ => throw new Exception(s"Unknown memory type for symbol $mem")
  }

  def memSizes(lhs: String, mem: Sym[_]): Unit = {
    val rank = mem match {
      case Op(alias: MemDenseAlias[_, _, _]) =>
        alias.ranges.zipWithIndex.foreach { case (rng, d) =>
          val series: Series[Idx] = rng.last
          emit(src"${lhs}_dim$d = ( (${series.end} - ${series.start} + ${series.step} - 1)/${series.step})")
        }
        alias.rank.length
      case Op(alloc: MemAlloc[_, _]) =>
        (1 to alloc.rank.length).foreach{d =>
          emit(src"# Parallelization in dimension #d")
          emit(src"${lhs}_dim$d = ${alloc.dims(d)}")
        }
        alloc.rank.length
      case _ => throw new Exception(s"Unknown memory type for symbol $mem")
    }
    emit(src"${lhs}_dims = [${(1 to rank).map{d => src"${lhs}_dim$d" }.mkString(",")}]")
    emit(src"${lhs}_pars = [${(1 to rank).map{d => src"${lhs}_P$d" }.mkString(",")}]")
  }

  override protected def emitEntry(block: Block[_]): Unit = {
    emit(src"#!/usr/bin/env python")
    emit(src"")
    emit(src"from sizes import *")
    emit(common)
    open(src"def main():")
    visitBlock(block)
    close("")
    open(src"""if __name__ == "__main__":""")
      emit(src"main()")
    close("")

    withGen(out, "sizes.py"){
      undefinedSyms.foreach{sym =>
        cmntHead()
        cmnt(s"Value ${sym.fullname}")
        cmntCtx(sym)
        cmntHead()
        val value = sym match{case Param(c) => c; case _ => 100 } // TODO: Choose some default value. Should warn?
        emit(src"$sym = $value")
        cmntFoot()
      }
    }
  }

  override def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(block) if lhs.isInnerControl =>
      val body = latencyOfPipe(block)
      cmntHead()
      cmnt(src"Inner Accel ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_time = $body + sync")
      emit(src"${lhs}_II = 1.0")
      cmntFoot()

    case AccelScope(block) =>
      visitBlock(block)
      cmntHead()
      cmnt(src"Outer Accel ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_stage_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}]")
      emit(src"${lhs}_time = control_model(1.0, 1.0, ${lhs}_stage_times, SEQUENTIAL)")
      emit(src"${lhs}_II = 1.0")
      cmntFoot()

    case ParallelPipe(_,block) =>
      visitBlock(block)
      cmntHead()
      cmnt(src"Parallel ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}]")
      emit(src"${lhs}_time = control_model(1.0, 1.0, ${lhs}_stages_times, PARALLEL)")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      cmntFoot()

    case UnitPipe(_, block) if lhs.isInnerControl =>
      val (body, blockII) = latencyAndInterval(block)
      val II = lhs.userII.getOrElse(blockII)
      cmntHead()
      cmnt(src"Inner Dummy Controller ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_time = $body + sync")
      emit(src"${lhs}_II = $II")
      cmntFoot()

    case OpForeach(ens,cchain,block,_) if lhs.isInnerControl =>
      val (body, blockII) = latencyAndInterval(block)
      val II = lhs.userII.getOrElse(blockII)

      cmntHead()
      cmnt(src"Inner Foreach ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      ctrHead(src"$lhs", cchain)
      emit(src"")
      emit(src"# Initiation Interval")
      emit(src"${lhs}_II = $II")
      emit(src"")
      emit(src"# Execution schedule [SEQUENTIAL | INNER_PIPELINE]")
      emit(src"${lhs}_schedule = ${schedule(lhs)}")
      emit(src"")
      cmntHead()
      nIters(src"$lhs", cchain, src"${lhs}_N", src"${lhs}_P")
      emit(src"${lhs}_L = $body")
      emit(src"${lhs}_time = inner_pipeline_runtime_model(${lhs}_N, ${lhs}_II, ${lhs}_L, ${lhs}_schedule)")
      cmntFoot()

    case OpReduce(ens, cchain, _, map, load, reduce, store, _,_,_) =>
      val (mapLat, mapII) = latencyAndInterval(map)

      val ldLat = latencyOfCycle(load)
      val reduceLat = latencyOfCycle(reduce)
      val storeLat = latencyOfCycle(store)

      val nodeLat = latencyOfPipe(reduce)

      val cycle = ldLat + reduceLat + storeLat

      val compilerII = Math.max(mapII, cycle)
      val II = lhs.userII.getOrElse(compilerII)

      cmntHead()
      cmnt(src"Inner Reduce ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      ctrHead(src"$lhs", cchain)
      emit(src"")
      emit(src"# Initiation interval")
      emit(src"${lhs}_II = $II")
      emit(src"")
      emit(src"# Execution schedule [SEQUENTIAL | INNER_PIPELINE]")
      emit(src"${lhs}_schedule = ${schedule(lhs)}")
      emit(src"")
      cmntHead()
      nIters(src"$lhs", cchain, src"${lhs}_N", src"${lhs}_P")
      emit(src"${lhs}_tree_depth = ceil(log(${lhs}_P, 2))")
      emit(src"${lhs}_map_L = $mapLat")
      emit(src"${lhs}_tree_L = $cycle + $nodeLat * ${lhs}_tree_depth")
      emit(src"${lhs}_L = ${lhs}_map_L + ${lhs}_tree_L")
      emit(src"${lhs}_time = inner_pipeline_runtime_model(${lhs}_N, ${lhs}_II, ${lhs}_L, ${lhs}_schedule)")
      cmntFoot()

    case StateMachine(ens, start, notDone, action, nextState) if lhs.isInnerControl =>
      val cont = latencyOfPipe(notDone)
      val act  = latencyOfPipe(action)
      val next = latencyOfPipe(nextState)

      cmntHead()
      cmnt(src"Inner State Machine ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"# Number of iterations of this state machine")
      emit(src"${lhs}_N = 100")
      emit(src"")
      cmntHead()
      emit(src"${lhs}_L = $cont + $act + $next")
      emit(src"${lhs}_II = ${lhs}_L")
      emit(src"${lhs}_time = inner_pipeline_runtime_model(${lhs}_N, ${lhs}_II, ${lhs}_L, SEQUENTIAL)")
      cmntFoot()

    case Switch(selects, body) if lhs.isInnerControl =>
      val (latency, ii) = latencyAndInterval(body)
      val II = lhs.userII.getOrElse(ii)
      cmntHead()
      cmnt(src"Inner If-Then-Else ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_L = $latency")
      emit(src"${lhs}_II = $II")
      emit(src"${lhs}_time = ${lhs}_L")
      cmntFoot()

    // --- Outer Control --- //

    case UnitPipe(_, block) if lhs.isOuterControl =>
      visitBlock(block)
      cmntHead()
      cmnt(src"Outer Dummy Controller ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}]")
      emit(src"${lhs}_N = 1.0")
      emit(src"${lhs}_II = 1.0")
      emit(src"${lhs}_time = control_model(1.0, 1.0, ${lhs}_stages_times, ${schedule(lhs)})")
      cmntFoot()

    case OpForeach(ens, cchain, block, iters) =>
      visitBlock(block)
      cmntHead()
      cmnt(src"Outer Foreach ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      ctrHead(src"$lhs", cchain)
      emit(src"# Execution schedule [SEQUENTIAL | OUTER_PIPELINE]")
      emit(src"${lhs}_schedule = ${schedule(lhs)}")
      emit(src"")
      cmntHead()
      nIters(src"$lhs", cchain, src"${lhs}_N", src"${lhs}_P")
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}]")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      emit(src"${lhs}_time = control_model(${lhs}_N, ${lhs}_II, ${lhs}_stages_times, ${lhs}_schedule)")
      cmntFoot()

    case OpReduce(ens, cchain, _, map, load, reduce, store, _, _, _) =>
      visitBlock(map)
      val ldLat = latencyOfCycle(load)
      val reduceLat = latencyOfCycle(reduce)
      val storeLat = latencyOfCycle(store)

      val nodeLat = latencyOfPipe(reduce)
      val cycle = ldLat + reduceLat + storeLat

      cmntHead()
      cmnt(src"Outer Reduce ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      ctrHead(src"$lhs", cchain)
      emit(src"# Execution schedule [SEQUENTIAL | OUTER_PIPELINE]")
      emit(src"${lhs}_schedule = ${schedule(lhs)}")
      emit(src"")
      cmntHead()
      nIters(src"$lhs", cchain, src"${lhs}_N", src"${lhs}_P")
      emit(src"${lhs}_tree_depth = ceil(log(${lhs}_P, 2))")
      emit(src"${lhs}_tree_L = $cycle + $nodeLat * ${lhs}_tree_depth")
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}, ${lhs}_tree_L]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}, $cycle]")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      emit(src"${lhs}_time = control_model(${lhs}_N, ${lhs}_II, ${lhs}_stages_times, ${lhs}_schedule)")
      cmntFoot()

    case OpMemReduce(ens, cchainMap, cchainRed, _, map, loadRes, loadAcc, reduce, storeAcc, _, _, _, _) =>
      visitBlock(map)
      val nodeLat = latencyOfPipe(reduce)
      val loadLat = latencyOfPipe(loadRes)
      val cycle   = latencyOfCycle(loadAcc) + latencyOfCycle(reduce) + latencyOfCycle(storeAcc)

      cmntHead()
      cmnt(src"Outer MemReduce ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      ctrHead(src"${lhs}_map", cchainMap)
      ctrHead(src"${lhs}_reduce", cchainRed)
      emit(src"# Execution schedule [SEQUENTIAL | OUTER_PIPELINE]")
      emit(src"${lhs}_schedule = ${schedule(lhs)}")
      emit(src"")
      cmntHead()
      nIters(src"${lhs}_map", cchainMap, src"${lhs}_Nmap", src"${lhs}_Pmap")
      nIters(src"${lhs}_reduce", cchainRed, src"${lhs}_Nreduce", src"${lhs}_Preduce")
      emit(src"${lhs}_tree_depth = ceil(log(${lhs}_P, 2))")
      emit(src"${lhs}_tree_L = $cycle + $loadLat + $nodeLat * ${lhs}_tree_depth")
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}, ${lhs}_tree_L]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}, $cycle]")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      emit(src"${lhs}_time = control_model(${lhs}_Nmap, ${lhs}_II, ${lhs}_stages_times, ${lhs}_schedule)")
      cmntFoot()

    case StateMachine(ens, start, notDone, action, nextState) =>
      val cont = latencyOfPipe(notDone)
      visitBlock(action)
      val next = latencyOfPipe(nextState)

      cmntHead()
      cmnt(src"Outer State Machine ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"# Number of iterations of the state machine")
      emit(src"${lhs}_N = 100")
      emit(src"")
      cmntHead()
      emit(src"${lhs}_stages_times = [$cont, ${lhs.children.map{c => src"${c}_time"}.mkString(", ")}, $next]")
      emit(src"${lhs}_II = ${lhs}_L")
      emit(src"${lhs}_time = control_model(${lhs}_N, ${lhs}_II, ${lhs}_stages_times, SEQUENTIAL)")
      cmntFoot()

    case op @ Switch(selects, body) =>
      visitBlock(body)
      cmntHead()
      cmnt(src"Outer If-Then-Else ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"# Branch probabilities")
      val nCases = op.cases.length.toDouble
      emit(src"${lhs}_probs = [${op.cases.indices.map{_ => 1.0/nCases}.mkString(", ")}]")
      emit(src"")
      cmntHead()
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}]")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      emit(src"${lhs}_time = ${lhs.children.zipWithIndex.map{case (c,i) => src"${c}_time * ${lhs}_probs[$i]"}.mkString(" + ")}")
      cmntFoot()

    case SwitchCase(body) =>
      visitBlock(body)
      cmntHead()
      cmnt(src"Outer If-Then-Else Case ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      emit(src"${lhs}_stages_times = [${lhs.children.map{c => src"${c}_time"}.mkString(", ")}]")
      emit(src"${lhs}_stages_IIs = [${lhs.children.map{c => src"${c}_II"}.mkString(", ")}]")
      emit(src"${lhs}_time = sum(${lhs}_stages_times)")
      emit(src"${lhs}_II = max(${lhs}_stages_IIs)")
      cmntFoot()

    case tx:DenseTransfer[_,_,_] =>
      val tp = if (tx.isStore) "store" else "load"
      cmntHead()
      cmnt(src"Dense $tp ${lhs.fullname}")
      cmntCtx(lhs)
      cmntHead()
      memPars(src"$lhs", tx.dram)
      emit(src"")
      cmntHead()
      memSizes(src"$lhs", tx.local)
      emit(src"${lhs}_wordBits = ${tx.A.nbits}")
      emit(src"${lhs}_II = 1.0")
      emit(src"${lhs}_time = memory_${tp}_runtime_model(${lhs}_dims, ${lhs}_pars, ${lhs}_wordBits)")
      cmntFoot()

    case _ => lhs.blocks.foreach{block => visitBlock(block) }
  }

}