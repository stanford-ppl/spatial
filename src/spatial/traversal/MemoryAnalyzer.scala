package spatial.traversal

import argon._
import argon.static.Printing
import argon.codegen.Codegen
import argon.passes.Pass
import poly.ISL
import models.AreaEstimator
import spatial.util.spatialConfig

import spatial.traversal.banking._
import spatial.metadata.memory._
import spatial.lang._
import spatial.metadata.memory.LocalMemories

case class MemoryAnalyzer(IR: State)(implicit isl: ISL, areamodel: AreaEstimator) extends Codegen { // Printing with Pass {
  private val strategy: BankingStrategy = ExhaustiveBanking()

  override val ext: String = "html"
  override val lang: String = "banking"
  override val entryFile: String = "decisions.html"


  override protected def emitEntry(block: Block[_]): Unit = gen(block)

  override def emitHeader(): Unit = {
    emit(s"""
  <!DOCTYPE html>
  <html>
  <head>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="http://code.jquery.com/mobile/1.4.5/jquery.mobile-1.4.5.min.css">
  <script src="http://code.jquery.com/jquery-1.11.3.min.js"></script>
  <script src="http://code.jquery.com/mobile/1.4.5/jquery.mobile-1.4.5.min.js"></script>
  </head><body>

    <div data-role="main" class="ui-content" style="overflow-x:scroll;">
      <h2>Banking Decisions for ${spatialConfig.name}</h2>
      <TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">
  """)
  }

  override def emitFooter(): Unit = {
    emit("</TABLE>")
    emit("</body>")
    emit("</html>")
  }

  private def report[C[_]](mem: Sym[_], conf: MemoryConfigurer[C], t: Double, totalTime: Double): Unit = {
    val (totalCost: Double, winningScheme) = conf.schemesInfo.map{scheme => 
      val cost = scheme._2.map(_._4.head).sum
      (cost, scheme._1.toString)
    }.toList.sortBy(_._1).headOption.getOrElse((0.0, ""))
    emit("")
    emit("""<TABLE BORDER="1" CELLPADDING="1" CELLSPACING="0"><td>""")
    val typ = "\\[.*".r.replaceAllIn(mem.tp.toString, "")
    emit(f"<h3>${mem.name.getOrElse(".")} ($typ): Cost ${totalCost}%.2f%% (${t*100/totalTime}%.1f%% of analysis time)</h3>")
    val coll = "data-role=\"collapsible\""
    emit(s"""<div $coll><h4> </h4>""")
      emit(s"""<TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">""")
      emit(s"""<br><font size = "2">Sym $mem: ${mem.ctx} <font color="grey">- ${mem.ctx.content.getOrElse("<???>")}</font></font>""")
      emit(s"""<br><font size = "2">Search time: ${t}ms</font>""")
      emit(s"""<br><font size = "2">Winning scheme: ${winningScheme}</font>""")

      emit(s"""<br><br><font size = "2">Effort:    ${mem.bankingEffort}</font>""")
      emit(s"""<br><font size = "2">BankingViews:   ${conf.bankViews}</font>""")
      emit(s"""<br><font size = "2">NStrictness:   ${conf.nStricts}</font>""")
      emit(s"""<br><font size = "2">AlphaStrictness:   ${conf.aStricts}</font>""")
      emit(s"""<br><font size = "2">DimensionDuplication: ${conf.dimensionDuplication}</font>""")
      emit("""<br><TABLE BORDER="1" CELLPADDING="1" CELLSPACING="0"><td>""")
      emit(s"<font size=4>Found ${conf.schemesInfo.toList.size} Alternative Schemes</font>")
      emit(s"""<div $coll><h5> </h5>""")
        conf.schemesInfo.foreach{scheme => 
          val cost = scheme._2.map(_._4.head).sum
          emit("<br>")
          emit(s"""<p><div style="padding: 10px; border: 1px;display:inline-block;background-color: #ccc">""")
          emit(f"""<br><font size = "3"><b>Scheme cost ${cost}%.2f%%: ${scheme._1}</b></font>""")
          emit(s"""<br>${scheme._2.toList.size} duplicates""")
          scheme._2.foreach{dup => 
            val banking = dup._1
            val histRaw = dup._2
            val aux = dup._3
            val breakdown:Seq[Double] = dup._4
            val hist = 
              if (histRaw.size > 1) (Seq("""<div style="display:grid;grid-template-columns: max-content max-content max-content"><div style="border: 1px solid;padding: 5px"><b>muxwidth</b></div> <div style="border: 1px solid;padding: 5px"><b># R lanes</b></div><div style="border: 1px solid;padding: 5px"><b># W Lanes</b></div>""") ++ histRaw.grouped(3).map{b => s"""<div style="border: 1px solid;padding: 5px">${b(0)}</div> <div style="border: 1px solid;padding: 5px">${b(1)}</div><div style="border: 1px solid;padding: 5px">${b(2)}</div>"""} ++ Seq("</div>")).mkString(" ")
              else ""
            emit(s"""<br>Banking Decision: $banking""")
            emit(s"""<br><br>Aux Nodes: ${aux.mkString(",")}""")
            emit(s"""<br>${hist}""")
            emit(f"<br>Breakdown: Mem = LUTs <b>${breakdown(1)}%.2f%%</b> FFs <b>${breakdown(2)}%.2f%%</b> BRAMs <b>${breakdown(3)}%.2f%%</b>, Aux Nodes = LUTs <b>${breakdown(4)}%.2f%%</b> FFs <b>${breakdown(5)}%.2f%%</b> BRAMs <b>${breakdown(6)}%.2f%%</b>")
          }
          emit(s"""</div></p>""")
        }
      emit("</TABLE></div>")
    emit(s"""</TABLE></div>""")
    emit(s"""<br>""")
    emit("")
  }

  override protected def process[R](block: Block[R]): Block[R] = {
    val enGen = config.enGen
    config.enGen = true
    inGen(out, "decisions.html") {
      emitHeader()
      run()
      enWarn = Some(false)  // Disable warnings after the first run
      emitFooter()
    }
    config.enGen = enGen
    block
  }

  protected def configurer[C[_]](mem: Sym[_]): MemoryConfigurer[C] = (mem match {
    case m:SRAM[_,_]    => new MemoryConfigurer(m, strategy)
    case m:RegFile[_,_] => new MemoryConfigurer(m, strategy)
    case m:LUT[_,_]     => new MemoryConfigurer(m, strategy)
    case m:LineBuffer[_] => new MemoryConfigurer(m, strategy)
    case m:FIFO[_]      => new FIFOConfigurer(m, strategy)  // No buffering
    case m:MergeBuffer[_] => new FIFOConfigurer(m, strategy)
    case m:LIFO[_]      => new FIFOConfigurer(m, strategy)  // No buffering
    case m:Reg[_]       => new MemoryConfigurer(m, strategy)
    case m:FIFOReg[_]       => new MemoryConfigurer(m, strategy)
    case m:StreamIn[_]  => new MemoryConfigurer(m, strategy)
    case m:StreamOut[_] => new MemoryConfigurer(m, strategy)
    case _ => throw new Exception(s"Don't know how to bank memory of type ${mem.tp}")
  }).asInstanceOf[MemoryConfigurer[C]]

  def run(): Unit = {
    val memories = LocalMemories.all.toSeq
    val entries = memories.map{m =>  //Seq[(Long, Sym[_], MemoryConfigurer, Double)]
      val startTime = System.currentTimeMillis()
      val conf = configurer(m)
      conf.configure()
      val t = System.currentTimeMillis() - startTime
      val totalCost = conf.schemesInfo.map{scheme => 
        scheme._2.map(_._4.head).sum
      }.toList.sorted.headOption.getOrElse(0.0)
      (t, m, conf, totalCost)
    }
    val totalTime = entries.map(_._1).sum
    emit(src"""<TD>""")
    entries.sortBy(_._4).reverse.foreach{case (t, m, conf, _) => 
      report(m, conf, t, totalTime)
    }
    emit(src"""</TD>""")
    memories.zip(entries.map(_._1)).sortBy(_._2).foreach{case (m, time) =>
      dbg(s"$m completed in: $time ms")
    }
  }
}
