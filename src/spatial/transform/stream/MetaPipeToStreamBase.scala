package spatial.transform.stream

import argon.{Sym, bug, dbgs}
import spatial.metadata.memory._
import spatial.metadata.control._

import scala.collection.mutable

trait MetaPipeToStreamBase {

  implicit def IR: argon.State


  def computeShifts(parFactors: Iterable[Int]) = {
    dbgs(s"Par Factors: $parFactors")
    (spatial.util.crossJoin((parFactors map { f => Range(0, f).toList }).toList) map {
      _.toList
    }).toList
  }

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
        localMems.add(mem)
      case stmt =>
        stmt.readMems intersect localMems foreach {
          mem =>
            lastWrite.get(mem) match {
              case Some(wr) =>
                states(mem)(wr).add(stmt)
              case None =>
                bug(s"Could not find writer for $mem")
                throw new Exception(s"Could not find writer for $mem in statement $stmt")
            }
        }

        stmt.writtenMems intersect localMems foreach {
          mem =>
            lastWrite(mem) = stmt
            states.getOrElseUpdate(mem, mutable.LinkedHashMap.empty)(stmt) = mutable.Set.empty
        }
    }

    new LinearizedUseData(states)
  }
}
