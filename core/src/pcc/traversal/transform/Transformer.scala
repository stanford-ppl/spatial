package pcc
package traversal
package transform

import pcc.data._

trait Transformer extends Pass {
  protected val f: Transformer = this

  def apply[T](x: T): T = (x match {
    case x: Sym[_]   => transformSym(x.asInstanceOf[Sym[T]])
    case x: Block[_] => transformBlock(x)
    case x: Seq[_]   => x.map{this.apply}
  }).asInstanceOf[T]

  def tx[T](block: Block[T]): () => T = () => inlineBlock(block).asInstanceOf[T]

  protected def transformSym[T](sym: Sym[T]): Sym[T]
  protected def transformBlock[T](block: Block[T]): Block[T] = {
    stageLambdaN(block.inputs)({ inlineBlock(block) }, block.options)
  }

  protected def inlineBlock[T](block: Block[T]): Sym[T]

  def transferMetadata(srcDest: (Sym[_],Sym[_])): Unit = {
    transferMetadata(srcDest._1, srcDest._2)
  }
  def transferMetadata(src: Sym[_], dest: Sym[_]): Unit = {
    dest.name = src.name
    dest.prevNames = (state.paddedPass(state.pass-1),s"$src") +: src.prevNames
    val data = metadata.all(src).flatMap{case (_,meta) =>
      mirror(meta) : Option[Metadata[_]]
    }
    metadata.addAll(dest, data)
  }

  final protected def transferMetadataIfNew[A](lhs: Sym[A])(tx: => Sym[A]): (Sym[A], Boolean) = {
    val lhs2 = tx
    if (lhs.isSymbol && lhs2.isSymbol && lhs.id <= lhs2.id) {
      transferMetadata(lhs -> lhs2)
      (lhs2, true)
    }
    else (lhs2, false)
  }

  final def mirror[M<:Metadata[_]](m: M): Option[M] = Option(m.mirror(f)).map(_.asInstanceOf[M])

  def mirror[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    implicit val tA: Sym[A] = rhs.tR
    log(s"$lhs = $rhs [Mirror]")
    rhs.setCtx(lhs.ctx)  // Update context prior to transforming
    val (lhs2,_) = transferMetadataIfNew(lhs){ rhs.mirrorNode(f).asSym }
    log(s"${stm(lhs2)}")
    lhs2
  }
}
