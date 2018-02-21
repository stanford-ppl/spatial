package core
package transform

import core.passes.Pass

import scala.collection.mutable

abstract class Transformer extends Pass {
  protected val f: Transformer = this

  def apply[T](x: T): T = (x match {
    case x: Sym[_]    => transformSym(x.asInstanceOf[Sym[T]])
    case x: Block[_]  => transformBlock(x)
    case x: Option[_] => x.map{this.apply}
    case x: Seq[_]    => x.map{this.apply}
    case x: Map[_,_]  => x.map{case (k,v) => f(k) -> f(v) }
    case x: mutable.Map[_,_] => x.map{case (k,v) => f(k) -> f(v) }
    case x: Product     => mirrorProduct(x)
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
    stageLambdaN(f(block.inputs))({ inlineBlock(block) }, block.options)
  }

  protected def inlineBlock[T](block: Block[T]): Sym[T]

  /**
    * Visit and perform some transformation `func` over all statements in the block.
    * @return the substitution for the block's result
    */
  final protected def inlineBlockWith[T](block: Block[T])(func: Seq[Sym[_]] => Sym[T]): Sym[T] = {
    state.logTab += 1
    val result = func(block.stms)
    state.logTab -= 1
    result
  }

  def transferMetadata(srcDest: (Sym[_],Sym[_])): Unit = transferMetadata(srcDest._1, srcDest._2)
  def transferMetadata(src: Sym[_], dest: Sym[_]): Unit = {
    dest.name = src.name
    dest.prevNames = (state.paddedPass(state.pass-1),s"$src") +: src.prevNames
    val data = metadata.all(src).filterNot{case (_,m) => m.skipOnTransform }
                                .flatMap{case (_,m) => mirror(m) : Option[Metadata[_]] }
    metadata.addAll(dest, data)
  }

  final protected def transferMetadataIfNew[A](lhs: Sym[A])(tx: => Sym[A]): (Sym[A], Boolean) = {
    val lhs2 = tx
    val shouldTransfer = (lhs.rhs.getID,lhs2.rhs.getID) match {
      case (Some(id1),Some(id2)) => id1 <= id2
      case _ => true
    }
    if (shouldTransfer) {
      transferMetadata(lhs -> lhs2)
      (lhs2, true)
    }
    else (lhs2, false)
  }

  final def mirror(m: Metadata[_]): Option[Metadata[_]] = Option(m.mirror(f)).map(_.asInstanceOf[Metadata[_]])

  def mirror[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    implicit val tA: Type[A] = rhs.tR
    implicit val ctx: SrcCtx = lhs.ctx
    //logs(s"$lhs = $rhs [Mirror]")
    val (lhs2,_) = try {
      transferMetadataIfNew(lhs){ tA.viewAsSym(stage(rhs.mirror(f))) }
    }
    catch {case t: Throwable =>
      bug(s"An error occurred while mirroring $lhs = $rhs")
      throw t
    }
    //logs(s"${stm(lhs2)}")
    lhs2
  }

  final def mirrorProduct[T<:Product](x: T): T = {
    // Note: this only works if the case class has no implicit parameters!
    val constructor = x.getClass.getConstructors.head
    val inputs = x.productIterator.map{x => f(x).asInstanceOf[Object] }.toSeq
    constructor.newInstance(inputs:_*).asInstanceOf[T]
  }

  final protected def mirrorSym[A](sym: Sym[A]): Sym[A] = sym match {
    case Op(rhs) => mirror(sym,rhs)
    case _ => sym
  }

  final protected def removeSym(sym: Sym[_]): Unit = {
    state.scope = state.scope.filterNot(_ == sym)
    state.impure = state.impure.filterNot(_ == sym)
  }
}
