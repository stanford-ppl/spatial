package spatial.report

import argon._
import argon.passes.Pass
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.modeling._
import utils.implicits.collections._

case class MemoryReporter(IR: State) extends Pass {
  override def shouldRun: Boolean = config.enInfo

  protected def process[S](block: Block[S]): Block[S] = { run(); block }

  def run(): Unit = {
    import scala.language.existentials

    val mems = LocalMemories.all.filter(!_.isCtrlBlackbox).map{case Stm(s,d) =>
      val area = areaModel.areaOf(s, d, inHwScope = true, inReduce = false)
      s -> area
    }.toSeq.sortWith((a,b) => a._2 < b._2)

    inGen(config.repDir, "Memories.report") {
      val total = mems.map(_._2).fold(NoArea){_+_}
      emit(s"---------------------------")
      emit(s"Estimated Total Memories: ")
      emit(s"---------------------------")
      val tab = (0 +: total.keys.map{k => k.length }).max
      total.foreach{(key,v) => if (v > 0) emit(s"  $key: ${" "*(key.length - tab)}$v") }
      emit(s"---------------------------")
      emit("\n\n")

      mems.foreach{case (mem,area) =>
        emit(s"---------------------------------------------------------------------")
        emit(s"Name: ${mem.fullname}")
        emit(s"Type: ${mem.tp}")
        emit(s"Src:  ${mem.ctx}")
        emit(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
        emit(s"---------------------------------------------------------------------")
        emit(s"Symbol:     ${stm(mem)}")
        val duplicates = mem.duplicates
        emit(s"Instances: ${duplicates.length}")
        area.foreach{case (k,v) => if (v > 0) emit(s"  $k: ${" "*(k.length - 10)}$v") }

        val readers = mem.readers
        val writers = mem.writers
        emit("\n")
        emit(s"Instance Summary: ")
        duplicates.zipWithIndex.foreach{case (inst,id) =>
          val Memory(banking,depth,padding,isAccum) = inst
          val banks  = banking.map(_.nBanks).mkString(", ")
          val format = if (banks.length == 1) "Flat" else "Hierarchical"
          emit(s"  #$id: Banked")
          emit(s"     Resource: ${inst.resource.name}")
          emit(s"     Depth:    $depth")
          emit(s"     Padding:  $padding")
          emit(s"     Accum:    $isAccum")
          emit(s"     Banks:    $banks <$format>")
          banking.foreach{grp => emit(s"       $grp") }
          emit(s"     Ports: ")

          def portStr(prefix: String, port: Option[Int], as: Iterable[Sym[_]], tp: String): Unit = {
            val p = port.map(_.toString).getOrElse("M")

            // Find all accesses connected to this buffer port
            val accesses: Iterable[(Sym[_],Seq[Int],Port)] = {
              as.filter{a => a.dispatches.values.exists(_.contains(id)) }
                .flatMap{a => a.ports(id).filter(_._2.bufferPort == port).map{case (unroll,pt) => (a,unroll,pt) } }
            }

            emit(s"$prefix  $p [Type:$tp]:")
            accesses.groupBy(_._3.muxPort).toSeq.sortBy(_._1).foreach{case (muxPort, muxAccs) =>
              emit(s"$prefix    - Mux Port #$muxPort: ")
              muxAccs.foreach{case (a,uid,pt) =>
                val line = a.ctx.content.map(_.trim).getOrElse(stm(a))
                emit(s"$prefix      ${stm(a)} {${uid.mkString(",")}}")
                emit(s"$prefix      $line (${a.ctx})")
                emit(s"$prefix        Directly Banked: ${a.isDirectlyBanked}")
                emit(s"$prefix        Port: <offset: ${pt.muxOfs}, castgroup: (${pt.castgroup.mkString(",")}), broadcast: (${pt.broadcast.mkString(",")})>")
                emit("")
              }
            }
          }

          (0 until inst.depth).foreach{p =>
            portStr("       ", Some(p), writers,"WR")
            portStr("       ", Some(p), readers,"RD")
          }
          if (inst.depth > 1) {
            portStr("       ", None, writers,"WR")
            portStr("       ", None, readers,"RD")
          }
        }

        emit("\n\n")
        emit(s"Control Tree: ")
        ctrlTree(readers ++ writers, None).foreach{x => emit(x) }

        emit(s"---------------------------------------------------------------------")
        emit("\n\n\n")
      }
    }

  }
}

