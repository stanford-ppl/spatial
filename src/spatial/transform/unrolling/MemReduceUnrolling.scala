package spatial.transform.unrolling

import argon._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import utils.tags.instrument
trait MemReduceUnrolling extends ReduceUnrolling {

  override def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A], mop: Boolean)(implicit ctx: SrcCtx): Sym[_] = rhs match {
    case op: OpMemReduce[a,c] =>
      val OpMemReduce(en,cchainMap,cchainRed,accum,func,loadRes,loadAcc,reduce,storeAcc,zero,fold,itersMap,itersRed,stopWhen) = op
      implicit val A: Bits[a] = op.A
      implicit val C: LocalMem[a,c] = op.C
      val accum2 = accumHack(accum.asInstanceOf[c[a]], loadAcc)
      val stopWhen2 = if (stopWhen.isDefined) Some(memories((stopWhen.get,0)).asInstanceOf[Reg[Bit]]) else stopWhen

      if (cchainMap.willFullyUnroll && cchainRed.willFullyUnroll) {
        fullyUnrollMemReduceAndMap[a,c](lhs,f(en),f(cchainMap),f(cchainRed),accum2,f(zero),fold,func,loadRes,loadAcc,reduce,storeAcc,itersMap,itersRed,stopWhen2, mop)
      }
      else if (cchainMap.willFullyUnroll) {
        fullyUnrollMemReduceMap[a,c](lhs,f(en),f(cchainMap),f(cchainRed),accum2,f(zero),fold,func,loadRes,loadAcc,reduce,storeAcc,itersMap,itersRed,stopWhen2, mop)
      }
      else if (cchainRed.willFullyUnroll) {
        fullyUnrollMemReduce[a,c](lhs,f(en),f(cchainMap),f(cchainRed),accum2,f(zero),fold,func,loadRes,loadAcc,reduce,storeAcc,itersMap,itersRed,stopWhen2, mop)
      }
      else {
        partiallyUnrollMemReduce[a,c](lhs,f(en),f(cchainMap),f(cchainRed),accum2,f(zero),fold,func,loadRes,loadAcc,reduce,storeAcc,itersMap,itersRed,stopWhen2, mop)
      }

    case _ => super.unrollCtrl(lhs, rhs, mop)
  }

  def fullyUnrollMemReduceAndMap[A,C[T]](
    lhs:       Sym[_],                // Original pipe symbol
    ens:       Set[Bit],              // Enables
    cchainMap: CounterChain,          // Map counterchain
    cchainRed: CounterChain,          // Reduction counterchain
    accum:     C[A],                  // Accumulator (external)
    ident:     Option[A],             // Optional identity value for reduction
    fold:      Boolean,               // Optional value to fold with reduction
    func:      Block[C[A]],           // Map function
    loadRes:   Lambda1[C[A],A],       // Load function for intermediate values
    loadAcc:   Lambda1[C[A],A],       // Load function for accumulator
    reduce:    Lambda2[A,A,A],        // Reduction function
    storeAcc:  Lambda2[C[A],A,Void],  // Store function for accumulator
    itersMap:  Seq[I32],              // Bound iterators for map loop
    itersRed:  Seq[I32],               // Bound iterators for reduce loop
    stopWhen:  Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], C: LocalMem[A,C], ctx: SrcCtx): Void = {
    logs(s"Unrolling accum-fold $lhs -> $accum")

    val mapLanes = FullUnroller(s"${lhs}_map", cchainMap, itersMap, isInnerLoop = false, mop)
    val redLanes = FullUnroller(s"${lhs}_red", cchainRed, itersRed, true, mop)
    val redType  = reduce.result.reduceType
    val intermed = func.result


    val blk = stageLambda1(accum){
      logs(s"[Accum-fold $lhs] Unrolling map")
      unrollWithoutResult(func, mapLanes)

      logs(s"[Accum-fold $lhs] Unrolling unit pipe reduction")
      redLanes.foreach{p =>
        logs(s"Lane #$p")
        itersRed.foreach{i => logs(s"  $i -> ${f(i)}") }
      }

      stageWithFlow(UnitPipe(enables ++ ens, stageBlock{
        unrollMemReduceAccumulate(lhs, accum, ident, intermed, fold, reduce, loadRes, loadAcc, storeAcc, redType, itersMap, itersRed, Nil, mapLanes, redLanes, mop)
      }, stopWhen)){lhs2 =>
        transferData(lhs,lhs2)
        lhs2.unrollBy = redLanes.Ps.product
      }
    
    }

    val lhs2 = stageWithFlow(UnitPipe(enables ++ ens, blk, stopWhen)){lhs2 =>
      transferData(lhs,lhs2)
      lhs2.unrollBy = mapLanes.Ps.product
    }
    //accumulatesTo(lhs2) = accum

    logs(s"[Accum-fold] Created reduce ${stm(lhs2)}")
    lhs2
  }

  def fullyUnrollMemReduce[A,C[T]](
    lhs:       Sym[_],                // Original pipe symbol
    ens:       Set[Bit],              // Enables
    cchainMap: CounterChain,          // Map counterchain
    cchainRed: CounterChain,          // Reduction counterchain
    accum:     C[A],                  // Accumulator (external)
    ident:     Option[A],             // Optional identity value for reduction
    fold:      Boolean,               // Optional value to fold with reduction
    func:      Block[C[A]],           // Map function
    loadRes:   Lambda1[C[A],A],       // Load function for intermediate values
    loadAcc:   Lambda1[C[A],A],       // Load function for accumulator
    reduce:    Lambda2[A,A,A],        // Reduction function
    storeAcc:  Lambda2[C[A],A,Void],  // Store function for accumulator
    itersMap:  Seq[I32],              // Bound iterators for map loop
    itersRed:  Seq[I32],               // Bound iterators for reduce loop
    stopWhen:  Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], C: LocalMem[A,C], ctx: SrcCtx): Void = {
    logs(s"Unrolling accum-fold $lhs -> $accum")

    val mapLanes = PartialUnroller(s"${lhs}_map", cchainMap, itersMap, isInnerLoop = false, mop)
    val redLanes = FullUnroller(s"${lhs}_red", cchainRed, itersRed, true, mop)
    val isMap2   = mapLanes.indices
    val mvs      = mapLanes.indexValids
    val start    = cchainMap.counters.map(_.start.asInstanceOf[I32])
    val redType  = reduce.result.reduceType
    val intermed = func.result

    logs(s"  Map iterators: $isMap2")
    mapLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersMap.foreach{i => logs(s"    $i -> ${f(i)}") }
    }

    val blk = stageLambda1(accum){
      logs(s"[Accum-fold $lhs] Unrolling map")
      unrollWithoutResult(func, mapLanes)

      logs(s"[Accum-fold $lhs] Unrolling unit pipe reduction")
      redLanes.foreach{p =>
        logs(s"Lane #$p")
        itersRed.foreach{i => logs(s"  $i -> ${f(i)}") }
      }

      stageWithFlow(UnitPipe(enables ++ ens, stageBlock{
        unrollMemReduceAccumulate(lhs, accum, ident, intermed, fold, reduce, loadRes, loadAcc, storeAcc, redType, itersMap, itersRed, start, mapLanes, redLanes, mop)
        // val foldValue = if (fold) Some( loadAcc.inline() ) else None
        // unrollReduceAccumulate[A,C](accum, values, mvalids(), ident, foldValue, reduce, loadAcc, storeAcc, isMap2.map(_.head), start, redLanes, isInner = false)
      }, stopWhen)){lhs2 =>
        transferData(lhs,lhs2)
        lhs2.unrollBy = redLanes.Ps.product
      }
    
    }

    val lhs2 = stageWithFlow(UnrolledReduce(enables ++ ens, cchainMap, blk, isMap2, mvs, stopWhen)){lhs2 =>
      transferData(lhs,lhs2)
    }
    //accumulatesTo(lhs2) = accum

    logs(s"[Accum-fold] Created reduce ${stm(lhs2)}")
    lhs2
  }


  def fullyUnrollMemReduceMap[A,C[T]](
    lhs:       Sym[_],                // Original pipe symbol
    ens:       Set[Bit],              // Enables
    cchainMap: CounterChain,          // Map counterchain
    cchainRed: CounterChain,          // Reduction counterchain
    accum:     C[A],                  // Accumulator (external)
    ident:     Option[A],             // Optional identity value for reduction
    fold:      Boolean,               // Optional value to fold with reduction
    func:      Block[C[A]],           // Map function
    loadRes:   Lambda1[C[A],A],       // Load function for intermediate values
    loadAcc:   Lambda1[C[A],A],       // Load function for accumulator
    reduce:    Lambda2[A,A,A],        // Reduction function
    storeAcc:  Lambda2[C[A],A,Void],  // Store function for accumulator
    itersMap:  Seq[I32],              // Bound iterators for map loop
    itersRed:  Seq[I32],               // Bound iterators for reduce loop
    stopWhen:  Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], C: LocalMem[A,C], ctx: SrcCtx): Void = {
    logs(s"Unrolling accum-fold $lhs -> $accum")

    val mapLanes = FullUnroller(s"${lhs}_map", cchainMap, itersMap, isInnerLoop = false, mop)
    val redLanes = PartialUnroller(s"${lhs}_red", cchainRed, itersRed, true, mop)
    val isRed2   = redLanes.indices
    val mvs      = mapLanes.indexValids
    val rvs      = redLanes.indexValids
    val redType  = reduce.result.reduceType
    val intermed = func.result

    logs(s"  Reduction iterators: $isRed2")
    redLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersRed.foreach{i => logs(s"    $i -> ${f(i)}") }
    }

    val blk = stageLambda1(accum){
      logs(s"[Accum-fold $lhs] Unrolling map")
      unrollWithoutResult(func, mapLanes)

      logs(s"[Accum-fold $lhs] Unrolling pipe-reduce reduction")

      val rBlk = stageBlock{
        logs(s"[Accum-fold $lhs] Unrolling map loads")
        unrollMemReduceAccumulate(lhs, accum, ident, intermed, fold, reduce, loadRes, loadAcc, storeAcc, redType, itersMap, itersRed, Nil, mapLanes, redLanes, mop)
      }

      stage(UnrolledForeach(Set.empty, cchainRed, rBlk, isRed2, rvs, None))
    }

    val lhs2 = stageWithFlow(UnitPipe(enables ++ ens, blk, stopWhen)){lhs2 =>
      transferData(lhs,lhs2)
      lhs2.unrollBy = mapLanes.Ps.product
    }
    //accumulatesTo(lhs2) = accum

    logs(s"[Accum-fold] Created reduce ${stm(lhs2)}")
    lhs2
  }

  def partiallyUnrollMemReduce[A,C[T]](
    lhs:       Sym[_],                // Original pipe symbol
    ens:       Set[Bit],              // Enables
    cchainMap: CounterChain,          // Map counterchain
    cchainRed: CounterChain,          // Reduction counterchain
    accum:     C[A],                  // Accumulator (external)
    ident:     Option[A],             // Optional identity value for reduction
    fold:      Boolean,               // Optional value to fold with reduction
    func:      Block[C[A]],           // Map function
    loadRes:   Lambda1[C[A],A],       // Load function for intermediate values
    loadAcc:   Lambda1[C[A],A],       // Load function for accumulator
    reduce:    Lambda2[A,A,A],        // Reduction function
    storeAcc:  Lambda2[C[A],A,Void],  // Store function for accumulator
    itersMap:  Seq[I32],              // Bound iterators for map loop
    itersRed:  Seq[I32],               // Bound iterators for reduce loop
    stopWhen:  Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], C: LocalMem[A,C], ctx: SrcCtx): Void = {
    logs(s"Unrolling accum-fold $lhs -> $accum")

    val mapLanes = PartialUnroller(s"${lhs}_map", cchainMap, itersMap, isInnerLoop = false, mop)
    val redLanes = PartialUnroller(s"${lhs}_red", cchainRed, itersRed, isInnerLoop = true, mop = mop)
    val isMap2   = mapLanes.indices
    val isRed2   = redLanes.indices
    val mvs      = mapLanes.indexValids
    val rvs      = redLanes.indexValids
    val start    = cchainMap.counters.map(_.start.asInstanceOf[I32])
    val redType  = reduce.result.reduceType
    val intermed = func.result

    logs(s"  Map iterators: $isMap2")
    mapLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersMap.foreach{i => logs(s"    $i -> ${f(i)}") }
    }

    logs(s"  Reduction iterators: $isRed2")
    redLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersRed.foreach{i => logs(s"    $i -> ${f(i)}") }
    }

    val blk = stageLambda1(accum){
      logs(s"[Accum-fold $lhs] Unrolling map")
      unrollWithoutResult(func, mapLanes)

      logs(s"[Accum-fold $lhs] Unrolling pipe-reduce reduction")

      val rBlk = stageBlock{
        logs(s"[Accum-fold $lhs] Unrolling map loads")
        unrollMemReduceAccumulate(lhs, accum, ident, intermed, fold, reduce, loadRes, loadAcc, storeAcc, redType, itersMap, itersRed, start, mapLanes, redLanes, mop)
      }

      stage(UnrolledForeach(Set.empty, cchainRed, rBlk, isRed2, rvs, stopWhen))
    }

    val lhs2 = stageWithFlow(UnrolledReduce(enables ++ ens, cchainMap, blk, isMap2, mvs, stopWhen)){lhs2 =>
      transferData(lhs,lhs2)
    }
    //accumulatesTo(lhs2) = accum

    logs(s"[Accum-fold] Created reduce ${stm(lhs2)}")
    lhs2
  }


  def unrollMemReduceAccumulate[A:Bits,C[T]](
    lhs: Sym[_],
    accum:  C[A],                 // Accumulator
    ident:  Option[A],            // Optional identity value
    intermed: Sym[_],
    fold:   Boolean,            // Optional fold value
    reduce: Lambda2[A,A,A],       // Reduction function
    loadRes:   Lambda1[C[A],A],      // Load function from accumulator
    loadAcc:   Lambda1[C[A],A],      // Load function from accumulator
    storeAcc:  Lambda2[C[A],A,Void], // Store function to accumulator
    redType: Option[ReduceFunction],
    itersMap:  Seq[I32],             // Iterators for entire reduction (used to determine when to reset)
    itersRed:  Seq[I32],             // Iterators for entire reduction (used to determine when to reset)
    start:  Seq[I32],             // Start for each iterator
    mapLanes: Unroller,
    redLanes: Unroller,
    mop: Boolean
  )(implicit ctx: SrcCtx): Void = {
    if (redLanes.isInstanceOf[FullUnroller]) {
      logs(s"Unsetting iter diff analysis on $accum")
      // TODO: Only want to unset metadata for readers/writers in the cycle inside the reduce function
      accum.asInstanceOf[Sym[_]].removeSegmentMapping
      accum.asInstanceOf[Sym[_]].removeIterDiff
      storeAcc.result.asInstanceOf[Sym[_]].removeSegmentMapping
      storeAcc.result.asInstanceOf[Sym[_]].removeIterDiff
      logs(s"loadacc result ${loadAcc.result}")
      loadAcc.result.asInstanceOf[Sym[_]].removeSegmentMapping
      loadAcc.result.asInstanceOf[Sym[_]].removeIterDiff
    }

    val mems = mapLanes.map{_ => memories((intermed,0)) } // TODO: Just use the first duplicate always?
    val mvalids = () => mapLanes.valids.map{_.andTree}

    val values: Seq[Seq[A]] = inReduce(redType,isInner = false){
      mapLanes.map{ case List(i) =>
        register(intermed -> mems(i))
        unroll(loadRes, redLanes)
      }
    }

    logs(s"[Accum-fold $lhs] Unrolling accum loads")
    val accValues = inReduce(redType,isInner = false){
      register(loadAcc.input -> accum)
      unroll(loadAcc, redLanes)
    }

    logs(s"[Accum-fold $lhs] Unrolling reduction trees and cycles")
    val results = redLanes.map{ lane =>
      val p = redLanes.ulanes.indexOf(lane)
      val laneValid = if (redLanes.valids(p).isEmpty) Bit(true) else redLanes.valids(p).andTree

      logs(s"Lane #$p:")
      val inputs = values.map(_.apply(p)) // The pth value of each vector load
      val valids = mvalids().map{mvalid => mvalid & laneValid }

      val accValue = accValues(p)

      val result = inReduce(redType,true){
        val treeResult = unrollReduceTree(inputs, valids, ident, reduce.toFunction2)

        val isFirst = mapLanes match {
          case unroller: PartialUnroller =>
            unroller.indices.map(_.head).zip(start).map { case (i, st) => i === st }.andTree
          case _ =>
            Bit(true)
        }
        

        if (fold || spatialConfig.ignoreParEdgeCases) {
          // FOLD: On first iteration, use value of accumulator value rather than zero
          //val accumOrFirst = math_mux(isFirst, init, accValue)
          reduce.reapply(treeResult, accValue)
        }
        else {
          // REDUCE: On first iteration, store result of tree, do not include value from accum
          val res2   = reduce.reapply(treeResult, accValue)
          val select = mux(isFirst, treeResult, res2)
          box(select).reduceType = redType
          select
        }
      }

      register(reduce.result -> result)  // Lane-specific substitution
      result
    }

    logs(s"[Accum-fold $lhs] Unrolling accumulator store")
    // Use a default substitution for the reduction result to satisfy the block scheduler
    accum.asInstanceOf[Sym[_]].setMemReduceAccum
    inReduce(redType,isInner = false){ isolateSubst(){
      register(storeAcc.inputA -> accum)
      register(reduce.result -> results.head)
      unroll(storeAcc, redLanes)
    }}

    void
  }

}
