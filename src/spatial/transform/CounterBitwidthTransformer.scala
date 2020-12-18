package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer

import spatial.lang._
import spatial.node._
import spatial.util.shouldMotionFromConditional
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.blackbox._

import utils.math.log2Up

import emul.FixedPoint

case class CounterBitwidthTransformer(IR: State) extends MutateTransformer
  with AccelTraversal {

  /** Calculate the least bitwidth required for an integer. */
  private def getBitwidth(x: Int): Int = log2Up(x.abs)

  /** Extract the content from Const and cast it to Int. */
  private def constToInt(x: Sym[_]): Int =
    if (x.isConst)
      x.c.get.asInstanceOf[FixedPoint].toInt
    else
      throw new Exception(s"$x is not a Const.")

  /** Create a new CounterNew object with compact bitwidth. */
  private def getOptimizedCounterNew(ctr: CounterNew[_]): CounterNew[_] = ctr match {
    case CounterNew(start, stop, step, par) =>
      // we take the largest magnitude of start and stop to decide the boundary of bitwidth
      val begin = constToInt(start)
      val end = constToInt(stop)
      val bits = math.max(getBitwidth(begin), getBitwidth(end))

      // TODO: Find a better way that can map bitwidth to the exact Fix type
      bits match {
        case 1  => 
          type T = Fix[TRUE,_2 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 2  => 
          type T = Fix[TRUE,_3 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 3  => 
          type T = Fix[TRUE,_4 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 4  => 
          type T = Fix[TRUE,_5 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 5  => 
          type T = Fix[TRUE,_6 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 6  => 
          type T = Fix[TRUE,_7 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 7  => 
          type T = Fix[TRUE,_8 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 8  => 
          type T = Fix[TRUE,_9 ,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 9  => 
          type T = Fix[TRUE,_10,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 10 => 
          type T = Fix[TRUE,_11,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 11 => 
          type T = Fix[TRUE,_12,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 12 => 
          type T = Fix[TRUE,_13,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 13 => 
          type T = Fix[TRUE,_14,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 14 => 
          type T = Fix[TRUE,_15,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 15 => 
          type T = Fix[TRUE,_16,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 16 => 
          type T = Fix[TRUE,_17,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 17 => 
          type T = Fix[TRUE,_18,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 18 => 
          type T = Fix[TRUE,_19,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 19 => 
          type T = Fix[TRUE,_20,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 20 => 
          type T = Fix[TRUE,_21,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 21 => 
          type T = Fix[TRUE,_22,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 22 => 
          type T = Fix[TRUE,_23,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 23 => 
          type T = Fix[TRUE,_24,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 24 => 
          type T = Fix[TRUE,_25,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 25 => 
          type T = Fix[TRUE,_26,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 26 => 
          type T = Fix[TRUE,_27,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 27 => 
          type T = Fix[TRUE,_28,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 28 => 
          type T = Fix[TRUE,_29,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 29 => 
          type T = Fix[TRUE,_30,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 30 => 
          type T = Fix[TRUE,_31,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case 31 => 
          type T = Fix[TRUE,_32,_0]
          CounterNew[T](begin.to[T], end.to[T], constToInt(step).to[T], par)
        case _  =>
          throw new Exception(s"Bit-width $bits is not supported")
    }
    case _ => ctr
  }

  /** Optimize a list of Counter. */
  private def getOptimizedCounters(ctrs: Seq[Counter[_]]): Seq[Counter[_]] = {
    ctrs.map { 
      case Op(ctr: CounterNew[_]) => stage(getOptimizedCounterNew(ctr))
    }
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => 
      inAccel { super.transform(lhs, rhs) }

    case OpForeach(ens, cchain, blk, iters, stopWhen) if inHw =>
      val newctrs = getOptimizedCounters(cchain.counters)
      val newcchain = stageWithFlow(CounterChainNew(newctrs)){ lhs2 => transferData(lhs, lhs2)}

      stageWithFlow(
        OpForeach(
          ens,
          newcchain,
          stageBlock{blk.stms.foreach(visit)},
          iters,
          stopWhen)
      ){lhs2 => transferData(lhs, lhs2)}

    case _ => 
      dbgs(s"visiting $lhs = $rhs");
      super.transform(lhs, rhs)
  }
}
