package spatial.report

import argon._
import argon.passes.Pass
import spatial.data._
import spatial.util._

case class MemoryReporter(IR: State) extends Pass {
  override def shouldRun: Boolean = config.enInfo

  protected def process[S](block: Block[S]): Block[S] = { run(); block }

  def run(): Unit = {
    import scala.language.existentials

    val mems = localMems.all.map{case Stm(s,d) =>
      val area = areaModel.areaOf(s, d, inHwScope = true, inReduce = false)
      s -> area
    }.toSeq.sortWith((a,b) => a._2 < b._2)

    withLog(config.logDir, "Memories.report") {
      val total = mems.map(_._2).fold(NoArea){_+_}
      dbgs(s"---------------------------")
      dbgs(s"Estimated Total Memories: ")
      dbgs(s"---------------------------")
      val tab = (0 +: total.keys.map{k => k.length }).max
      total.foreach{(key,v) => if (v > 0) dbgs(s"  $key: ${" "*(key.length - tab)}$v") }
      dbg(s"---------------------------")
      dbgs(s"\n\n")

      mems.foreach{case (mem,area) =>
        dbg(s"---------------------------------------------------------------------")
        dbg(s"Name: ${mem.fullname}")
        dbg(s"Type: ${mem.tp}")
        dbg(s"Src:  ${mem.ctx}")
        dbg(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
        dbg(s"---------------------------------------------------------------------")
        dbg(s"Symbol:     ${stm(mem)}")
        val duplicates = duplicatesOf(mem)
        dbg(s"Instances: ${duplicates.length}")
        area.foreach{case (k,v) => if (v > 0) dbgs(s"  $k: ${" "*(k.length - 10)}$v") }

        val readers = readersOf(mem)
        val writers = writersOf(mem)
        dbgs(s"\n")
        dbg(s"Instance Summary: ")
        duplicates.zipWithIndex.foreach{case (inst,i) =>
          val Memory(banking,depth,isAccum) = inst
          val banks  = banking.map(_.nBanks).mkString(", ")
          val format = if (banks.length == 1) "Flat" else "Hierarchical"
          dbg(s"  #$i: Banked")
          dbg(s"     Resource: ${inst.resource.name}")
          dbg(s"     Depth:    $depth")
          dbg(s"     Accum:    $isAccum")
          dbg(s"     Banks:    $banks <$format>")
          banking.foreach{grp => dbgs(s"       $grp") }
          dbg(s"     Ports: ")
          def portStr(port: Option[Int], as: Iterable[Sym[_]], tp: String): Iterator[String] = {
            as.iterator.filter{a => dispatchOf(a).values.exists(_.contains(i)) }
                       .filter{a => portsOf(a).values.exists(_.bufferPort == port) }
                       .map{a => s"  ${port.map(_.toString).getOrElse("M")}: [$tp] ${a.ctx.content.map(_.trim).getOrElse(stm(a)) } [${a.ctx}]"}
          }

          (0 until inst.depth).foreach{p =>
            val lines = portStr(Some(p), writers,"WR") ++ portStr(Some(p), readers,"RD")
            lines.foreach{line => dbg(s"       $line")}
          }
          if (inst.depth > 1) {
            val lines = portStr(None, writers, "WR") ++ portStr(None, readers, "RD")
            lines.foreach { line => dbg(s"       $line") }
          }
        }

        dbg(s"---------------------------------------------------------------------")
        dbg("\n\n\n")
      }
    }

  }
}

