package spatial.transform.unrolling

import argon._
import spatial.lang._
import spatial.node._
import argon.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig
import utils.tags.instrument

trait ReduceUnrolling extends UnrollingBase {

  override def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A], mop: Boolean)(implicit ctx: SrcCtx): Sym[_] = rhs match {
    case op :OpReduce[a] =>
      val OpReduce(ens,cchain,accum,map,load,reduce,store,ident,fold,iters,stopWhen) = op
      implicit val A: Bits[a] = op.A
      val accum2 = accumHack(accum, load)
      accum2.isInnerAccum = lhs.isInnerControl

      val stopWhen2 = if (stopWhen.isDefined) Some(memories((stopWhen.get,0)).asInstanceOf[Reg[Bit]]) else stopWhen
      if (cchain.willFullyUnroll) {
        fullyUnrollReduce(lhs, f(ens), f(cchain), accum2, ident, fold, load, store, map, reduce, iters, stopWhen2, mop)
      }
      else {
        partiallyUnrollReduce(lhs, f(ens), f(cchain), accum2, ident, fold, load, store, map, reduce, iters, stopWhen2, mop)
      }

    case _ => super.unrollCtrl(lhs, rhs, mop)
  }


  def fullyUnrollReduce[A](
    lhs:    Sym[_],
    ens:    Set[Bit],
    cchain: CounterChain,
    accum:  Reg[A],
    ident:  Option[A],
    fold:   Option[A],
    load:   Lambda1[Reg[A],A],
    store:  Lambda2[Reg[A],A,Void],
    func:   Block[A],
    reduce: Lambda2[A,A,A],
    iters:  Seq[I32],
    stopWhen: Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], ctx: SrcCtx): Void = {
    dbgs(s"Fully unrolling reduce $lhs")
    val mapLanes = FullUnroller(s"$lhs", cchain, iters, lhs.isInnerControl, mop)
    val redLanes = UnitUnroller(s"${lhs}_reduce", isInnerLoop=true)
    val rfunc = reduce.toFunction2

    val pipe = stageWithFlow(UnitPipe(enables ++ ens, stageBlock{
      inLanes(redLanes) {
        val foldValid: Option[Bit] = fold.map(_ => Bit(true))
        val valids: () => Seq[Bit] = () => foldValid.toSeq ++ mapLanes.valids.map { vs => vs.andTree }
        val values: Seq[A] = unroll(func, mapLanes)
        val inputs: Seq[A] = fold.toSeq ++ values

        if (lhs.isOuterControl) {
          dbgs("Fully unrolling outer reduce")
          Pipe {
            val result = unrollReduceTree[A](inputs, valids(), ident, rfunc)
            store.reapply(accum, result)
          }
        }
        else {
          dbgs("Fully unrolling inner reduce")
          val result = unrollReduceTree[A](inputs, valids(), ident, rfunc)
          store.reapply(accum, result)
        }
      }
    })){lhs2 => transferData(lhs,lhs2) }
    dbgs(s"Created unit pipe ${stm(pipe)}")
    pipe
  }

  def partiallyUnrollReduce[A](
    lhs:    Sym[_],                 // Original pipe symbol
    ens:    Set[Bit],               // Enables
    cchain: CounterChain,           // Counterchain
    accum:  Reg[A],                 // Accumulator
    ident:  Option[A],              // Optional identity value for reduction
    fold:   Option[A],              // Optional value to fold with reduction
    load:   Lambda1[Reg[A],A],      // Load function for accumulator
    store:  Lambda2[Reg[A],A,Void], // Store function for accumulator
    func:   Block[A],               // Map function
    reduce: Lambda2[A,A,A],         // Reduce function
    iters:  Seq[I32],                // Bound iterators for map loop
    stopWhen: Option[Reg[Bit]],
    mop: Boolean
  )(implicit A: Bits[A], ctx: SrcCtx): Void = {
    dbgs(s"Unrolling reduce $lhs -> $accum")
    val mapLanes = PartialUnroller(s"$lhs", cchain, iters, lhs.isInnerControl, mop)
    val inds2 = mapLanes.indices
    val vs = mapLanes.indexValids
    val start = cchain.counters.map(_.start.asInstanceOf[I32])

    val blk = stageLambda1(accum) {
      dbgs(s"Unrolling reduce map $lhs -> $accum")
      val valids: () => Seq[Bit] = () => mapLanes.valids.map{_.andTree}
      val values: Seq[A] = unroll(func, mapLanes)

      if (lhs.isOuterControl) {
        dbgs("Unrolling unit pipe reduce")
        stage(UnitPipe(enables, stageBlock{
          unrollReduceAccumulate[A,Reg](accum, values, valids(), ident, fold, reduce, load, store, inds2.map(_.head), start, isInner = false)
        }))
      }
      else {
        dbgs("Unrolling inner reduce")
        unrollReduceAccumulate[A,Reg](accum, values, valids(), ident, fold, reduce, load, store, inds2.map(_.head), start, isInner = true)
      }
    }

    val lhs2 = stageWithFlow(UnrolledReduce(enables ++ ens, cchain, blk, inds2, vs, stopWhen)){lhs2 => transferData(lhs,lhs2) }
    //accumulatesTo(lhs2) = accum
    dbgs(s"Created reduce ${stm(lhs2)}")
    accum.accumType = AccumType.Reduce
    lhs2
  }

  // Hack to get the accumulator duplicate from the original and the loadAccum block for this reduction
  // Assumes that the accumulator corresponds to exactly one duplicate
  def accumHack[T](orig: Sym[T], load: Block[_]): T = orig match {
    case Op(MemDenseAlias(_,mems,_)) =>
      if (mems.length > 1) throw new Exception(s"Accumulation on aliased memories is not yet supported")
      else accumHack(mems.head.asInstanceOf[Sym[T]], load)

    case _ =>
      val contents = load.nestedStms
      val readers = orig.readers
      readers.find{reader => contents.contains(reader) } match {
        case Some(reader) =>
          val mapping = reader.dispatches
          if (mapping.isEmpty) throw new Exception(s"No dispatch found in reduce for accumulator $orig")
          val dispatch = mapping.head._2.head
          if (!memories.contains((orig,dispatch))) throw new Exception(s"No duplicate found for accumulator $orig")
          memories((orig,dispatch)).asInstanceOf[T]

        case None => throw new Exception(s"No reader found in reduce for accumulator $orig")
      }
  }

  def unrollReduceTree[A:Bits](
    inputs: Seq[A],
    valids: Seq[Bit],
    ident:  Option[A],
    rfunc: (A,A) => A
  )(implicit ctx: SrcCtx): A = ident match {
    case Some(z) =>
      val validInputs = inputs.zip(valids).map{case (in,v) => mux(v, in, z) }
      validInputs.reduceTree{ rfunc }

    case None =>
      // ASSUMPTION: If any values are invalid, they are at the end of the list (corresponding to highest index values)
      // TODO[2]: This may be incorrect if we parallelize by more than the innermost iterator
      inputs.zip(valids).reduceTree{case ((x, x_en), (y, y_en)) =>
        (mux(y_en, rfunc(x,y), x), x_en | y_en) // res is valid if x or y is valid
      }._1
  }


  def unrollReduceAccumulate[A:Bits,C[T]](
    accum:  C[A],                 // Accumulator
    inputs: Seq[A],               // Symbols to be reduced
    valids: Seq[Bit],             // Data valid bits corresponding to inputs
    ident:  Option[A],            // Optional identity value
    fold:   Option[A],            // Optional fold value
    reduce: Lambda2[A,A,A],       // Reduction function
    load:   Lambda1[C[A],A],      // Load function from accumulator
    store:  Lambda2[C[A],A,Void], // Store function to accumulator
    iters:  Seq[I32],             // Iterators for entire reduction (used to determine when to reset)
    start:  Seq[I32],             // Start for each iterator
    isInner: Boolean
  )(implicit ctx: SrcCtx): Void = {
    val redLanes = UnitUnroller(s"${accum}_accum", isInnerLoop = true)

    inLanes(redLanes) {
      val redType = reduce.result match {
        case Op(FixFMA(_,_,_)) => Some(FixPtFMA)
        case Op(FixAdd(_,_)) => Some(FixPtSum)
        case _ => None
      }
      reduce.result.reduceType = redType

      val treeResult = inReduce(redType, isInner) {
        unrollReduceTree[A](inputs, valids, ident, reduce.toFunction2)
      }

      val result: A = inReduce(redType, isInner) {
        dbgs(s"Inlining load function in reduce")
        val accValue = load.reapply(accum)
        val isFirst = iters.zip(start).map { case (i, st) => i === st }.andTree

        if (spatialConfig.ignoreParEdgeCases) {
          reduce.reapply(treeResult, accValue)
        }
        else fold match {
          // FOLD: On first iteration, use init value rather than zero
          case Some(init) =>
            val accumOrFirst: A = mux(isFirst, init, accValue)
            box(accumOrFirst).reduceType = redType
            reduce.reapply(treeResult, accumOrFirst)

          // REDUCE: On first iteration, store result of tree, do not include value from accum
          // TODO: Could also have third case where we use ident instead of loaded value. Is one better?
          case None =>
            val res2 = reduce.reapply(treeResult, accValue)
            val select = mux(isFirst, treeResult, res2)
            box(select).reduceType = redType

            dbgs(s"isFirst: ${stm(isFirst)}")
            dbgs(s"res2:    ${stm(res2)}")
            dbgs(s"select:  ${stm(select)}")

            select
        }
      }

      inReduce(redType, isInner) {
        dbgs(s"Store: $result to $accum")

        val res = store.reapply(accum, result)

        dbgs(s"Completed store (symbol $res)")
        res
      }
    }
  }

}
