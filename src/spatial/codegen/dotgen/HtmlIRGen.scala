package spatial.codegen.dotgen

import argon._
import utils.io.files._

trait HtmlIRCodegen extends argon.codegen.Codegen {

  val lang = "info"

  def ext = "html"

  protected def emitEntry(block: Block[_]): Unit = {
    ret(block)
  }
  override def clearGen(): Unit = {
    deleteFiles(s"$out$sep$entryFile")
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    emitElem("h2", "Global Metadata", "id" -> "global")
    globals.foreach{(k,v) => emitMeta(v) }
    super.postprocess(block)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    emitElem("h3", qdef(lhs), "id" -> s"$lhs")
    emitMeta(lhs)

    val binds = rhs.binds.filter(_.isBound)
    if (binds.nonEmpty || rhs.blocks.nonEmpty) {
      emitElem("table", "border"->3, "cellpadding"->10, "cellspacing"->10) {
        emitElem("tbody") {
          if (binds.nonEmpty) {
            emitElem("tr") {
              emitElem("td") {
                emitElem("h2", "binds")
                binds.foreach{b =>
                  emitElem("h3", qdef(b), "id" -> s"$b")
                  emitMeta(b)
                }
              }
            }
          }

          if (rhs.blocks.nonEmpty) {
            rhs.blocks.zipWithIndex.foreach { case (blk, i) =>
              emitElem("tr") {
                emitElem("td") {
                  emitElem("h2", s"block $i")
                  ret(blk)
                }
              }
            }
          }
        }
      }
    }
  }

  def emitMeta(lhs: Sym[_]): Unit = {
    lhs.name.foreach{ name => 
      text(src"${elem("strong","Name")}: ${name}")
    }
    text(src"${elem("strong","SrcCtx")}: ${lhs.ctx}")
    text(src"${elem("strong","Type")}: ${lhs.tp}")
    if (lhs.prevNames.nonEmpty) {
      val aliases = lhs.prevNames.map{case (tx,alias) => s"$tx: $alias" }.mkString(", ")
      text(src"${elem("strong","Aliases")}: $aliases")
    }
    metadata.all(lhs).foreach{case (k,m) => emitMeta(m) }
  }

  def emitMeta(data:Data[_]) = data match {
    case data:Effects => text(src"${elem("strong",data.getClass.getSimpleName)}: ${data}")
    case data:Product if data.productIterator.size == 1 => 
      text(src"${elem("strong",data.getClass.getSimpleName)}: ${data.productIterator.next}")
    case data =>  text(src"${data.getClass.getSimpleName}: $data")
  }

  def qdef(sym:Sym[_]) = sym.op match {
    case Some(op) => src"$sym = $op"
    case None => src"$sym"
  }

  def text(msg:String) = {
    emitElem("text",msg + "<br>")
  }

  def elem(header:String, msg:String, attrs:(String, Any)*):String = {
    var at = attrs.map { case (key,value) => s"$key=$value" }.mkString(" ")
    at = if (attrs.nonEmpty) " " + at else ""
    s"<$header$at>$msg</$header>"
  }

  def emitElem(header:String, msg:String, attrs:(String, Any)*):Unit = {
    emit(elem(header, msg, attrs:_*))
  }

  def emitElem(header:String, attrs:(String, Any)*)(block: => Unit):Unit = {
    var at = attrs.map { case (key,value) => s"$key=$value" }.mkString(" ")
    at = if (attrs.nonEmpty) " " + at else ""
    emit(s"<$header$at>")
    block
    emit(s"</$header>")
  }

}

