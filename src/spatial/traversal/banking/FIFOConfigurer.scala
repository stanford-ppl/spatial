package spatial.traversal
package banking

import argon._
import poly.ISL
import models.AreaEstimator
import utils.implicits.collections._
import utils.math._

import spatial.issues.UnbankableGroup
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

class FIFOConfigurer[+C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State, isl: ISL, areamodel: AreaEstimator)
  extends MemoryConfigurer[C](mem,strategy)
{

  override def requireConcurrentPortAccess(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val lca = LCA(a.access, b.access)
    (a.access == b.access && (a.unroll != b.unroll || a.access.isVectorAccess)) ||
      lca.isPipeLoop || lca.isOuterStreamLoop ||
      (lca.isInnerSeqControl && lca.isFullyUnrolledLoop) ||
      lca.isParallel
  }

  private def computePorts(groups: Set[Set[AccessMatrix]]): Map[AccessMatrix,Port] = {
    groups.zipWithIndex.flatMap{case (group,muxPort) =>
      // TODO: Broadcast possible for FIFOs?
      import scala.math.Ordering.Implicits._
      val (vec,uroll) = group.partition(_.access.isVectorAccess)
      val urollMap = uroll.toSeq.sortBy(_.unroll).zipWithIndex.map{case (matrix,muxOfs) =>
        val port = Port(
          bufferPort = Some(0),
          muxPort    = muxPort,
          muxOfs     = muxOfs,
          castgroup  = Seq(0),
          broadcast  = Seq(0)
        )
        matrix -> port
      }
      // Assumes only one vector access will be in each group.  Can this be wrong?
      val vecMap = vec.toSeq.sortBy(_.unroll).map{matrix => 
        val port = Port(
          bufferPort = Some(0),
          muxPort    = muxPort,
          muxOfs     = 0,
          castgroup  = Seq.fill(vec.size)(0),
          broadcast  = Seq.fill(vec.size)(0)
        )
        matrix -> port
      }
      urollMap ++ vecMap
    }.toMap
  }

  def groupsAreConcurrent(grps: Set[Set[AccessMatrix]]): Boolean = grps.cross(grps).exists{case (g1,g2) =>
    g1 != g2 && g1.cross(g2).exists{case (a,b) => !mem.shouldIgnoreConflicts && requireConcurrentPortAccess(a,b) }
  }

  override protected def bankGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Either[Issue,Seq[Instance]] = {
    val haveConcurrentReads = groupsAreConcurrent(rdGroups)
    val haveConcurrentWrites = groupsAreConcurrent(wrGroups)

    if (haveConcurrentReads || haveConcurrentWrites) {
      Left(UnbankableGroup(mem,rdGroups.flatten,wrGroups.flatten))
    }
    else {
      val nStricts: Seq[NStrictness] = Seq(NPowersOf2, NBestGuess, NRelaxed)
      val aStricts: Seq[AlphaStrictness] = Seq(AlphaPowersOf2, AlphaBestGuess, AlphaRelaxed)
      val dimensionDuplication: Seq[RegroupDims] = RegroupHelper.regroupNone
      val bankingOptionsIds: List[List[Int]] = combs(List(List.tabulate(nStricts.size){i => i}, List.tabulate(aStricts.size){i => i}, List.tabulate(dimensionDuplication.size){i => i}))
      val attemptDirectives: Seq[BankingOptions] = bankingOptionsIds.map{ addr => BankingOptions(Flat(rank), nStricts(addr(0)), aStricts(addr(1)), dimensionDuplication(addr(2))) }
    
      val bankings = strategy.bankAccesses(mem, rank, rdGroups, wrGroups, attemptDirectives, depth = 1).head._2
      if (bankings.nonEmpty) {
        val banking = bankings.head._2
        val bankingCosts = cost(banking, depth = 1, rdGroups, wrGroups)._4.head
        val ports = computePorts(rdGroups) ++ computePorts(wrGroups)

        Right(Seq(Instance(
          reads  = rdGroups,
          writes = wrGroups,
          ctrls  = Set.empty,
          metapipe = None,
          banking  = banking,
          depth    = 1,
          cost     = bankingCosts,
          ports    = ports,
          padding  = mem.getPadding.getOrElse(Seq(0)),
          darkVolume = banking.head.darkVolume,
          accType  = AccumType.None
        )))
      }
      else Left(UnbankableGroup(mem,rdGroups.flatten,wrGroups.flatten))
    }
  }

}
