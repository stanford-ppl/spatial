package spatial.transform.streamify

import argon._
import argon.tags.struct
import forge.tags.stateful
import spatial.lang._

import scala.collection.{mutable => cm}
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.access._
import spatial.metadata.transform._
import spatial.util.TransformUtils._
import spatial.util._


//
//object StreamingControlBundle {
//  @stateful def getFIFOSizing(edge: DependencyEdge): Int = edge match {
//    case inferred@InferredDependencyEdge(src, dst, edgeType) =>
//      val inFlight = inferred.lca.children.filter {
//        child =>
//          (child.nestedReadMems ++ child.nestedWrittenMems) contains inferred.mem
//      }
//      inFlight.size
//  }
//}
//
//case class StreamingControlBundle(lhs: Sym[_])(implicit state: argon.State) {
//  dbgs(s"Ancestors of $lhs = ${lhs.ancestors} [${lhs.isStreamPrimitive}]")
//  val allCtrlers = lhs.ancestors.dropRight(if (lhs.isStreamPrimitive) 1 else 0)
//  val allChains = allCtrlers.flatMap(_.cchains)
//  val allCounters = allChains.flatMap(_.counters)
//  val allIters = allCounters.flatMap(_.iter)
//
//  // The FIFODepth just needs to be able to accommodate bodyLatency, or two vectorized writes from the ctrlers.
//  val fifoDepth: I32 = {
//    val latencyEpsilon = 8
//    val bodyLatency = if (lhs.isStreamPrimitive) {
//      lhs.children.size + latencyEpsilon
//    } else {
//      scala.math.ceil(lhs.bodyLatency.head).toInt + latencyEpsilon
//    }
//    val ctrlerWritePar = 2 * allChains.last.counters.map(_.nIters match {
//      case Some(x) => x.toInt
//      case None => 1
//    }).product
//
//    val depth = scala.math.max(bodyLatency, ctrlerWritePar)
//    dbgs(s"FIFO Depth for $lhs = $depth")
//    I32(depth)
//  }
//  val iterFIFO: FIFO[PseudoIters[Vec[PseudoIter]]] = {
//    implicit def vecBitsEV: Bits[Vec[PseudoIter]] = Vec.bits[PseudoIter](allCounters.size)
//
//    val iterFIFO: FIFO[PseudoIters[Vec[PseudoIter]]] = FIFO[PseudoIters[Vec[PseudoIter]]](fifoDepth)
//    iterFIFO.explicitName = s"IterFIFO_$lhs"
//
//    iterFIFO
//  }
//
//  val lastIterType: VecStructType[Sym[_]] = VecStructType(allChains.map(_.counters.head).map { ctr => ctr.iter.get -> Bit(false) })
//  val releaseLastFIFO: FIFO[Vec[Bit]] = {
//    implicit def vecBitsEV: Bits[Vec[Bit]] = lastIterType.bitsEV
//
//    val releaseLastFIFO = FIFO[Vec[Bit]](fifoDepth)
//    releaseLastFIFO.explicitName = s"ReleaseLastFIFO_$lhs"
//    releaseLastFIFO
//  }
//
////  val intakeComms = tokenComms.filter(_.dst == lhs.toCtrl)
////  val releaseComms = tokenComms.filter(_.src == lhs.toCtrl)
////
////  val tokenStreamType = commsToVecStructType(intakeComms)
////  val tokenEnableType = VecStructType(intakeComms.map(_.mem).distinct.map(mem => (mem, Bit(false))))
//
////  assert(tokenStreamType.isEmpty == tokenEnableType.isEmpty, s"Mismatch between $tokenStreamType and its enabler $tokenEnableType")
////
////  dbgs(s"Token Enable Type: $tokenEnableType")
////  val (tokenFIFO, finishTokenFIFO, bypassTokenFIFO) = if (!tokenStreamType.isEmpty) {
////    implicit def bEV: Bits[Vec[Bit]] = tokenStreamType.bitsEV
////
////    val tokenFIFO = FIFO[Vec[Bit]](fifoDepth)
////    tokenFIFO.explicitName = s"TokenFIFO_$lhs"
////    val finishTokenFIFO = FIFO[Vec[Bit]](fifoDepth)
////    finishTokenFIFO.explicitName = s"FinishTokenFIFO_$lhs"
////    val bypassTokenFIFO = FIFO[Vec[Bit]](fifoDepth)
////    bypassTokenFIFO.explicitName = s"BypassTokenFIFO_$lhs"
////    (Some(tokenFIFO), Some(finishTokenFIFO), Some(bypassTokenFIFO))
////  } else {
////    (None, None, None)
////  }
////
////  val acquireFIFO = if (!tokenEnableType.isEmpty) {
////    implicit def bEV: Bits[Vec[Bit]] = tokenEnableType.bitsEV
////
////    val acquireFIFO = FIFO[Vec[Bit]](fifoDepth)
////    acquireFIFO.explicitName = s"AcquireFIFO_$lhs"
////    Some(acquireFIFO)
////  } else None
//
//  val tokenSourceFIFO = FIFO[Bit](fifoDepth)
//  tokenSourceFIFO.explicitName = s"TokenSourceFIFO_$lhs"
//}