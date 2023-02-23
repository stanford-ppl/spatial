package spatial.transform.streamify

import argon.{Op, Sym}
import spatial.lang._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._
object StreamifyUtils {
  case class InnerGroup(accesses: Set[Sym[_]]) {
    val parent = spatial.util.assertUniqueAndTake(accesses.map(_.parent))
  }

  /**
    * Computes groups of accesses that can conflict
    * @param mem the memory we're interested in
    * @return a sequence of Sets of possibly conflicting InnerGroups
    */
  @forge.tags.stateful def getConflictGroups(mem: Sym[_]): Seq[Set[InnerGroup]] = {
    lazy val splitByEnqDeq = {
      val accessTypes = mem.accesses.groupBy {
        case Op(_: Enqueuer[_]) => true
        case Op(_: Dequeuer[_, _]) => false
      }
      accessTypes.values.toSeq
    }
    val accessClasses = mem match {
      case _: FIFO[_] => splitByEnqDeq
      case _: StreamOut[_] => splitByEnqDeq
      case _: StreamIn[_] => splitByEnqDeq
      case _ =>
        Seq(mem.accesses)
    }
    val grouped = accessClasses.map {
      accesses =>
        accesses.groupBy(_.parent).values.map(InnerGroup).toSet
    }
    grouped.filter(_.size > 1).toList
  }
}
