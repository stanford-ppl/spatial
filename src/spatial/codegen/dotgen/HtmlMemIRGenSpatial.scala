package spatial.codegen.dotgen

import argon._
import spatial.metadata.memory._
import utils.io.files._

case class HtmlMemIRGenSpatial(override val IR: State) extends HtmlIRGenSpatial(IR) {

  override def entryFile: String = s"Mem.$ext"

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (lhs.isMem) {
      super.gen(lhs, rhs)
      if (lhs.writers.nonEmpty) {
        emitElem("table", "border"->3, "cellpadding"->10, "cellspacing"->10) {
          emitElem("tbody") {
            emitElem("tr") {
              emitElem("th", s"writers(${lhs.writers.size})")
              lhs.writers.foreach { access =>
                emitElem("td") {
                  super.gen(access, access.op.get)
                }
              }
            }
          }
        }
      }
      if (lhs.readers.nonEmpty) {
        emitElem("table", "border"->3, "cellpadding"->10, "cellspacing"->10) {
          emitElem("tbody") {
            emitElem("tr") {
              emitElem("th", s"readers(${lhs.readers.size})")
              lhs.readers.foreach { access =>
                emitElem("td") {
                  super.gen(access, access.op.get)
                }
              }
            }
          }
        }
      }
    } else {
      rhs.blocks.foreach(ret)
    }
  }

}

