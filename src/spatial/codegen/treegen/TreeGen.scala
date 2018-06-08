package spatial.codegen.treegen

import argon._
import argon.codegen.Codegen

import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.{spatialConfig => cfg}
import spatial.codegen.naming.NamedCodegen

case class TreeGen(IR: State) extends NamedCodegen {
  override val ext: String = "html"
  override val lang: String = "html"
  override val entryFile: String = "controller_tree.html"
  val table_init = """<TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">"""

  override def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case _:Control[_] => printControl(lhs, rhs)
    case _ => rhs.blocks.foreach{blk => gen(blk) }
  }

  override protected def emitEntry(block: Block[_]): Unit = gen(block)

  def printControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    val cchain = lhs.cchains.headOption.map(_.toString)
    val isLeaf = lhs.isInnerControl && lhs.rawChildren.isEmpty
    val line   = lhs.ctx.content.getOrElse("<?:?:?>")

    open(s"""<!--Begin $lhs -->""")
    val isFSM = lhs match {case Op(_: StateMachine[_]) => " FSM"; case _ => ""}
    emit(s"""<TD><font size = "6">${lhs.schedule} $isFSM<font size = "4"> (${lhs.level})</font>""")
    emit(s"""<br><font size = "2">$ctx</font>""")
    emit(s"""<br><font size = "2">$line</font>""")
    emit(s"""<br><font size = "1"><b>$lhs = $rhs</b></font>""")
    if (cchain.isDefined) emit(s"""<br><font size = "1">Counter: ${cchain.get}</font>""")

    // if (!inner & !collapsible) {emit(s"""${" "*html_tab}<br><font size = "1"><b>**Stages below are route-through (think of cycle counts as duty-cycles)**</b></font>""")}
    emit("")
    if (!isLeaf) {
      val coll = "data-role=\"collapsible\""
      emit(s"""<div $coll>""")
      emit(s"""<h4> </h4>$table_init""")
    }

    print_stream_info(lhs)
    rhs.blocks.foreach{blk => gen(blk) }

    if (!isLeaf) {
      emit(s"""</TABLE></div>""")
    }
    close(s"""</TD><!-- Close $name -->""")
  }

  def print_stream_info(sym: Sym[_]): Unit = {
    val listens = getReadStreams(sym.toCtrl).map{a => s"$a" }
    val pushes  = getWriteStreams(sym.toCtrl).map{a => s"$a" }
    if (listens.nonEmpty || pushes.nonEmpty) {
      emit(s"""<div style="border:1px solid black">Stream Info<br>""")
      if (listens.nonEmpty) emit(s"""<p align="left">----->$listens""")
      if (listens.nonEmpty && pushes.nonEmpty) emit(s"<br>")
      if (pushes.nonEmpty) emit(s"""<p align="right">$pushes----->""")
      emit(s"""</div>""")
    }
  }



  override def emitHeader(): Unit = {
    val options = {
      (if (!cfg.enableAsyncMem) Seq("SyncMem") else Nil) ++
      (if (cfg.enableRetiming)  Seq("Retimed") else Nil)
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
      <h2>Controller Diagram for ${cfg.name} (Options: $optionStr)</h2>
  <TABLE BORDER="3" CELLPADDING="10" CELLSPACING="10">""")
  }

  override def emitFooter(): Unit = {
    emit (s"""
  </TABLE>
  </body>
  </html>""")
  }
}
