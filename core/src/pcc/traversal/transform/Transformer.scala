package pcc.traversal
package transform

import pcc.core._
import pcc.data._

abstract class Transformer extends Pass {
  protected val f: Transformer = this

  def apply[T](x: T): T = (x match {
    case x: Sym[_]   => transformSym(x.asInstanceOf[Sym[T]])
    case x: Block[_] => transformBlock(x)
    case x: Seq[_]   => x.map{this.apply}
    case x: Map[_,_] => x.map{case (k,v) => f(k) -> f(v) }
    case x: Iterable[_] => x.map{this.apply}
    case x: Int => x
    case x: Long => x
    case x: Float => x
    case x: Double => x
    case _ => throw new Exception(s"No rule for mirroring ${x.getClass}")
  }).asInstanceOf[T]

  def tx[T](block: Block[T]): () => T = () => inlineBlock(block).asInstanceOf[T]

  protected def transformSym[T](sym: Sym[T]): Sym[T]
  protected def transformBlock[T](block: Block[T]): Block[T] = {
    stageLambdaN(block.inputs)({ inlineBlock(block) }, block.options)
  }

  protected def inlineBlock[T](block: Block[T]): Sym[T]

  def transferMetadata(srcDest: (Sym[_],Sym[_])): Unit = transferMetadata(srcDest._1, srcDest._2)
  def transferMetadata(src: Sym[_], dest: Sym[_]): Unit = {
    dest.name = src.name
    dest.prevNames = (state.paddedPass(state.pass-1),s"$src") +: src.prevNames
    val data = metadata.all(src).flatMap{case (_,m) => mirror(m) : Option[Metadata[_]] }
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
    implicit val ctx: SrcCtx = lhs.ctx
    log(s"$lhs = $rhs [Mirror]")
    val (lhs2,_) = try {
      transferMetadataIfNew(lhs){ stage(rhs.mirror(f)).asSym }
    }
    catch {case t: Throwable =>
      bug(s"An error occurred while mirroring $lhs = $rhs")
      throw t
    }
    log(s"${stm(lhs2)}")
    lhs2
  }


}
