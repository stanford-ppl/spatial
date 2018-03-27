package argon
package codegen

import utils.io.files

trait FileGen extends Codegen {
  protected def emitEntry(block: Block[_]): Unit

  override protected def process[R](block: Block[R]): Block[R] = {
    inGen(out, entryFile) {
      emitEntry(block)
    }
    block
  }

  def clearGen(): Unit = {
    files.deleteExts(out, ext, recursive = true)
  }

  def emitHeader(): Unit = { }
  def emitFooter(): Unit = { }

  override protected def preprocess[R](b: Block[R]): Block[R] = {
    clearGen()
    emitHeader()
    super.preprocess(b)
  }

  override protected def postprocess[R](b: Block[R]): Block[R] = {
    emitFooter()
    super.postprocess(b)
  }
}
