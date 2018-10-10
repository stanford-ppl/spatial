package spatial.codegen.treegen

import argon._

import spatial.metadata.control._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.codegen.naming.NamedCodegen
import spatial.traversal.AccelTraversal
import spatial.util.modeling.scrubNoise

case class TreeGen(IR: State) extends NamedCodegen with AccelTraversal {
  override val ext: String = "html"
  backend = "tree"
  override val lang: String = "html"
  override val entryFile: String = "controller_tree.html"
  val table_init = """<TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">"""

  override def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AccelScope(func)     => inAccel{ printControl(lhs,rhs) }
    case _:Control[_] if inHw => printControl(lhs, rhs)
    case _ => rhs.blocks.foreach{blk => gen(blk) }
  }

  override def quoteConst(tp: Type[_], c: Any): String = c.toString

  override protected def emitEntry(block: Block[_]): Unit = gen(block)

  def printControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    val cchain = lhs.cchains.headOption.map(_.toString)
    val isLeaf = lhs.isInnerControl && lhs.rawChildren.isEmpty
    val line   = lhs.ctx.content.getOrElse("<?:?:?>")

    open(s"""<!--Begin $lhs -->""")
    val isFSM = lhs match {case Op(_: StateMachine[_]) => " FSM"; case _ => ""}
    emit(s"""<TD><font size = "6">${lhs.schedule} $isFSM<font size = "4"> (${lhs.level})</font>""")
    emit(s"""<br><font size = "2">${lhs.ctx}</font>""")
    emit(s"""<br><font size = "2">$line</font>""")
    val ii = scrubNoise(lhs.II).toInt
    val lat = scrubNoise(lhs.bodyLatency.sum).toInt
    val attentionII = if (ii > 1) src"<b>II=$ii</b>" else src"II=$ii"
    if (lhs.isInnerControl) emit(s"""<p><mark style="border:1px; border-style:solid; border-color:black; padding: 1px; background: #ccc"><font size = "2">Latency=${lat},  ${attentionII}</font></mark></p>""")
    emit(s"""<br><font size = "1"><b>${lhs}${lhs._name} = $rhs</b></font>""")
    if (cchain.isDefined) emit(s"""<br><font size = "1">Counter: ${cchain.get}</font>""")

    // if (!inner & !collapsible) {emit(s"""${" "*html_tab}<br><font size = "1"><b>**Stages below are route-through (think of cycle counts as duty-cycles)**</b></font>""")}
    emit("")
    if (!isLeaf) {
      val coll = "data-role=\"collapsible\""
      emit(s"""<div $coll>""")
      emit(s"""<h4> </h4>$table_init""")
    }

    rhs.blocks.foreach{blk => gen(blk) }

    if (!isLeaf) {
      emit(s"""</TABLE></div>""")
    }
    print_stream_info(lhs)
    close(s"""</TD><!-- Close $name -->""")
  }

  def print_stream_info(sym: Sym[_]): Unit = {
    val listens = getReadStreams(sym.toCtrl).map{a => s"$a" }
    val pushes  = getWriteStreams(sym.toCtrl).map{a => s"$a" }
    if (listens.nonEmpty || pushes.nonEmpty) {
      emit(s"""<div style="border:1px solid black"><font size = "2">Stream Info</font><br><font size = "1"> """)
      if (listens.nonEmpty) emit(s"""<p align="left">----->$listens""")
      if (listens.nonEmpty && pushes.nonEmpty) emit(s"<br>")
      if (pushes.nonEmpty) emit(s"""<p align="right">$pushes----->""")
      emit(s"""</font></div>""")
    }
  }



  override def emitHeader(): Unit = {
    val options = {
      (if (!spatialConfig.enableAsyncMem) Seq("SyncMem") else Nil) ++
      (if (spatialConfig.enableRetiming)  Seq("Retimed") else Nil)
    }
    val optionStr = if (options.isEmpty) "None" else options.mkString(", ")

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
      <h2>Controller Diagram for ${spatialConfig.name} (Options: $optionStr)</h2>
  <TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">""")
  }

  override def emitFooter(): Unit = {
    emit (s"""
  </TABLE>
  </body>
  </html>""")
  }
}
