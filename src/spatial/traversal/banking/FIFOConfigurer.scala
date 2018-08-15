package spatial.traversal
package banking

import argon._
import poly.ISL
import utils.implicits.collections._

import spatial.issues.UnbankableGroup
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

class FIFOConfigurer[+C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State, isl: ISL)
  extends MemoryConfigurer[C](mem,strategy)
{

  override def requireConcurrentPortAccess(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val lca = LCA(a.access, b.access)
    (a.access == b.access && a.unroll != b.unroll) ||
      lca.isPipeLoop || lca.isOuterStreamLoop ||
      (lca.isInnerSeqControl && lca.isFullyUnrolledLoop) ||
      lca.isParallel
  }

  private def computePorts(groups: Set[Set[AccessMatrix]]): Map[AccessMatrix,Port] = {
    val muxSize = groups.map(_.size).maxOrElse(0)

    groups.zipWithIndex.flatMap{case (group,muxPort) =>
      // TODO: Broadcast possible for FIFOs?
      import scala.math.Ordering.Implicits._
      group.toSeq.sortBy(_.unroll).zipWithIndex.map{case (matrix,muxOfs) =>
        val port = Port(
          bufferPort = Some(0),
          muxPort    = muxPort,
          muxSize    = muxSize,
          muxOfs     = muxOfs,
          castgroup  = Seq(0),
          broadcast  = Seq(0)
        )
        matrix -> port
      }
    }.toMap
  }

  def groupsAreConcurrent(grps: Set[Set[AccessMatrix]]): Boolean = grps.cross(grps).exists{case (g1,g2) =>
    g1 != g2 && g1.cross(g2).exists{case (a,b) => requireConcurrentPortAccess(a,b) }
  }

  override protected def bankGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Either[Issue,Instance] = {
    val haveConcurrentReads = groupsAreConcurrent(rdGroups)
    val haveConcurrentWrites = groupsAreConcurrent(wrGroups)

    if (haveConcurrentReads || haveConcurrentWrites) {
      Left(UnbankableGroup(mem,rdGroups.flatten,wrGroups.flatten))
    }
    else {
      val bankings = strategy.bankAccesses(mem, rank, rdGroups, wrGroups, Seq(FLAT_BANKS))
      if (bankings.nonEmpty) {
        val banking = bankings.head
        val bankingCosts = cost(banking, depth = 1)
        val ports = computePorts(rdGroups) ++ computePorts(wrGroups)

        Right(Instance(
          reads  = rdGroups,
          writes = wrGroups,
          ctrls  = Set.empty,
          metapipe = None,
          banking  = banking,
          depth    = 1,
          cost     = bankingCosts,
          ports    = ports,
          accType  = AccumType.None
        ))
      }
      else Left(UnbankableGroup(mem,rdGroups.flatten,wrGroups.flatten))
    }
  }

}
