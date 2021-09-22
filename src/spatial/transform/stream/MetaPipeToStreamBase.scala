package spatial.transform.stream

import argon.{Block, Sym, bug, dbgs}
import spatial.metadata.memory._
import spatial.metadata.control._

import scala.collection.mutable

trait MetaPipeToStreamBase {

  implicit def IR: argon.State

  case class MemoryWriteData(writer: Sym[_], readers: Set[Sym[_]])

  class LinearizedUseData(val data: mutable.Map[Sym[_], mutable.LinkedHashMap[Sym[_], mutable.Set[Sym[_]]]]) {
    //    def memories = data.keys.toSeq

    // Internally store as a set of MemoryWriteDatas.
    val dataMap: Map[Sym[_], Seq[MemoryWriteData]] = {
      (data map {
        case (mem, wrData) =>
          val dataChain = wrData map {
            case (writer, readers) =>
              MemoryWriteData(writer, readers.toSet)
          }
          mem -> dataChain.toSeq
      }).toMap
    }
  }

  def computeLinearizedUses(stmts: Seq[Sym[_]]) = {
    val lastWrite = mutable.Map[Sym[_], Sym[_]]()
    val localMems = mutable.Set[Sym[_]]()

    // mem -> writer -> [readers]
    val states = mutable.Map[Sym[_], mutable.LinkedHashMap[Sym[_], mutable.Set[Sym[_]]]]()

    stmts foreach {
      case mem if mem.isMem =>
        dbgs(s"Adding mem: $mem")
        localMems.add(mem)
      case stmt =>
        dbgs(s"stmt: $stmt = ${stmt.op}")
        (stmt.effects.reads diff stmt.effects.writes) intersect localMems foreach {
          mem =>
            dbgs(s"Registering Reader: $stmt <- $mem")
            lastWrite.get(mem) match {
              case Some(wr) =>
                states(mem)(wr).add(stmt)
              case None =>
                bug(s"Could not find writer for $mem")
                throw new Exception(s"Could not find writer for $mem in statement $stmt")
            }
        }

        stmt.effects.writes intersect localMems foreach {
          mem =>
            dbgs(s"Registering Writer: $stmt -> $mem")
            lastWrite(mem) = stmt
            states.getOrElseUpdate(mem, mutable.LinkedHashMap.empty)(stmt) = mutable.Set.empty
        }
    }

    dbgs(s"Local Mems: $localMems")

    new LinearizedUseData(states)
  }

  def computeNonlocalUses(ctrl: Sym[_]) = {
    // If a memory is only read/written by a single child, then it's fine.
    // If a memory is read/written by multiple children, then we need to pass tokens around.
    val readMems = ctrl.effects.reads
    val writtenMems = ctrl.effects.writes
    val stmts = ctrl.blocks.flatMap(_.stms)
    val externalMems = (readMems union writtenMems) diff stmts.toSet
    dbgs(s"External Memories: $externalMems")
    val memUseCounts = (externalMems map {
      mem =>
        mem -> (stmts count { stmt => (stmt.effects.reads union stmt.effects.writes) contains mem })
    }).toMap

    val singleUseMemories = (memUseCounts filter {case (_, v) => v == 1}).keySet
    val multiUseMemories = (memUseCounts filter {case (_, v) => v > 1}).keySet

    dbgs(s"Single Use Memories: $singleUseMemories")
    dbgs(s"Multi Use Memories: $multiUseMemories")

    // For single use memories, we leave them alone.
    // For Multi use memories, we create a cycle of readers and writers to that memory.
    val multiUses = mutable.Map[Sym[_], mutable.ArrayBuffer[Sym[_]]]()
    stmts foreach {
      stmt =>
        val modifiedMultiUse = (stmt.effects.reads union stmt.effects.writes) intersect multiUseMemories
        modifiedMultiUse foreach {
          mem => multiUses.getOrElseUpdate(mem, mutable.ArrayBuffer.empty).append(stmt)
        }
    }
    (multiUses map {case (k, v) => k -> v.toList}).toMap
  }
}
