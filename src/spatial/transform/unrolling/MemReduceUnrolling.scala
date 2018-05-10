package spatial.transform.unrolling

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.spatialConfig
import utils.tags.instrument

trait MemReduceUnrolling extends ReduceUnrolling {

  override def unrollCtrl[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[_] = rhs match {
    case op: OpMemReduce[a,c] =>
      val OpMemReduce(en,cchainMap,cchainRed,accum,func,loadRes,loadAcc,reduce,storeAcc,zero,fold,itersMap,itersRed) = op
      implicit val A: Bits[a] = op.A
      implicit val C: LocalMem[a,c] = op.C
      val accum2 = accumHack(accum.asInstanceOf[c[a]], loadAcc)

      unrollMemReduce[a,c](lhs,f(en),f(cchainMap),f(cchainRed),accum2,f(zero),fold,func,loadRes,loadAcc,reduce,storeAcc,itersMap,itersRed)

    case _ => super.unrollCtrl(lhs, rhs)
  }

  def unrollMemReduce[A,C[T]](
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
    itersRed:  Seq[I32]               // Bound iterators for reduce loop
  )(implicit A: Bits[A], C: LocalMem[A,C], ctx: SrcCtx): Void = {
    logs(s"Unrolling accum-fold $lhs -> $accum")

    val mapLanes = PartialUnroller(cchainMap, itersMap, isInnerLoop = false)
    val reduceLanes = PartialUnroller(cchainRed, itersRed, true)
    val isMap2   = mapLanes.indices
    val isRed2   = reduceLanes.indices
    val mvs      = mapLanes.indexValids
    val rvs      = reduceLanes.indexValids
    val start    = cchainMap.counters.map(_.start.asInstanceOf[I32])
    val redType  = reduce.result.reduceType
    val intermed = func.result

    logs(s"  Map iterators: $isMap2")
    mapLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersMap.foreach{i => logs(s"    $i -> ${f(i)}") }
    }
    logs(s"  Reduction iterators: $isRed2")
    reduceLanes.foreach{p =>
      logs(s"    Lane #$p")
      itersRed.foreach{i => logs(s"    $i -> ${f(i)}") }
    }

    val blk = stageLambda1(accum){
      logs(s"[Accum-fold $lhs] Unrolling map")
      unrollWithoutResult(func, mapLanes)
      val mems = mapLanes.map{_ => memories((intermed,0)) } // TODO: Just use the first duplicate always?

      val mvalids = () => mapLanes.valids.map{_.andTree}

      if (cchainRed.isUnit) {
        logs(s"[Accum-fold $lhs] Unrolling unit pipe reduction")
        reduceLanes.foreach{p =>
          logs(s"Lane #$p")
          itersRed.foreach{i => logs(s"  $i -> ${f(i)}") }
        }

        stage(UnitPipe(enables ++ ens, stageBlock{
          val values = inReduce(redType,true){
            mapLanes.map{_ =>
              reduceLanes.inLane(0){ loadRes.inline() }
            }
          }
          val foldValue = if (fold) Some( loadAcc.inline() ) else None
          unrollReduceAccumulate[A,C](accum, values, mvalids(), ident, foldValue, reduce, loadAcc, storeAcc, isMap2.map(_.head), start, isInner = false)
          void
        }))
      }
      else {
        logs(s"[Accum-fold $lhs] Unrolling pipe-reduce reduction")

        val rBlk = stageBlock{
          logs(s"[Accum-fold $lhs] Unrolling map loads")
          //logs(c"  memories: $mems")

          val values: Seq[Seq[A]] = inReduce(redType,false){
            mapLanes.map{i =>
              register(intermed -> mems(i))
              unroll(loadRes, reduceLanes)
            }
          }

          logs(s"[Accum-fold $lhs] Unrolling accum loads")
          val accValues = inReduce(redType,false){
            register(loadAcc.input -> accum)
            unroll(loadAcc, reduceLanes)
          }

          logs(s"[Accum-fold $lhs] Unrolling reduction trees and cycles")
          val results = reduceLanes.map{p =>
            val laneValid = reduceLanes.valids(p).andTree

            logs(s"Lane #$p:")
            val inputs = values.map(_.apply(p)) // The pth value of each vector load
            val valids = mvalids().map{mvalid => mvalid & laneValid }

            val accValue = accValues(p)

            val result = inReduce(redType,true){
              val treeResult = unrollReduceTree(inputs, valids, ident, reduce.toFunction2)
              val isFirst = isMap2.map(_.head).zip(start).map{case (i,st) => i === st }.andTree

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
          inReduce(redType,false){ isolateSubst{
            register(storeAcc.inputA -> accum)
            register(reduce.result -> results.head)
            unroll(storeAcc, reduceLanes)
          }}
          void
        }

        stage(UnrolledForeach(Set.empty, cchainRed, rBlk, isRed2, rvs))
      }
    }

    val lhs2 = stage(UnrolledReduce(enables ++ ens, cchainMap, blk, isMap2, mvs))
    //accumulatesTo(lhs2) = accum

    logs(s"[Accum-fold] Created reduce ${stm(lhs2)}")
    lhs2
  }

}
