package argon
package transform

import argon.passes.Pass
import utils.tags.instrument

import scala.collection.mutable

abstract class Transformer extends Pass {
  protected val f: Transformer = this

  object F {
    def unapply[T](x: T): Option[T] = Some(f(x))
  }

  // Default rules for mirroring
  // NOTE: This doesn't currently work for mirroring Products with implicit constructor arguments
  def apply[T](x: T): T = {
    val y = (x match {
      case x: Mirrorable[_]    => mirrorMirrorable(x)
      case x: Sym[_]           => transformSym(x)
      case x: Lambda1[a,_]     => transformBlock(x).asLambda1[a]
      case x: Lambda2[a,b,_]   => transformBlock(x).asLambda2[a,b]
      case x: Lambda3[a,b,c,_] => transformBlock(x).asLambda3[a,b,c]
      case x: Block[_]         => transformBlock(x)
      case x: Option[_]        => x.map{this.apply}
      case x: Seq[_]           => x.map{this.apply}
      case x: Map[_,_]         => x.map{case (k,v) => f(k) -> f(v) }
      case x: mutable.Map[_,_] => x.map{case (k,v) => f(k) -> f(v) }
      case x: Product          => mirrorProduct(x)
      case x: Iterable[_]      => x.map{this.apply}
      case x: Char => x
      case x: Byte => x
      case x: Short => x
      case x: Int => x
      case x: Long => x
      case x: Float => x
      case x: Double => x
      case x: Boolean => x
      case x: String => x
      case _ =>
        if (config.enDbg) warn(s"No explicit mirroring rule for type ${x.getClass}")
        x
    }).asInstanceOf[T]

    if (y.isInstanceOf[Invalid]) throw new Exception(s"Used removed symbol in mirroring of $x")
    y
  }

  def tx[T](block: Block[T]): () => T = () => inlineBlock(block).asInstanceOf[T]

  protected def transformSym[T](sym: Sym[T]): Sym[T]
  protected def transformBlock[T](block: Block[T]): Block[T] = {
    stageScope(f(block.inputs),block.options){ inlineBlock(block) }
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
    implicit val tA: Type[A] = rhs.R
    implicit val ctx: SrcCtx = lhs.ctx
    //logs(s"$lhs = $rhs [Mirror]")
    val (lhs2,_) = try {
      transferMetadataIfNew(lhs){
        tA.boxed(stage( mirrorNode(rhs) ))
      }
    }
    catch {case t: Throwable =>
      bug(s"An error occurred while mirroring $lhs = $rhs")
      throw t
    }
    //logs(s"${stm(lhs2)}")
    lhs2
  }

  def mirrorNode[A](rhs: Op[A]): Op[A] = rhs.mirror(f)

  final def mirrorMirrorable(x: Mirrorable[_]): Mirrorable[_] = {
    x.mirror(f).asInstanceOf[Mirrorable[_]]
  }
  final def mirrorProduct[T<:Product](x: T): T = {
    // Note: this only works if the case class has no implicit parameters!
    val constructors = x.getClass.getConstructors
    if (constructors.isEmpty) x
    else {
      try {
        val constructor = x.getClass.getConstructors.head
        val inputs = x.productIterator.map { x => f(x).asInstanceOf[Object] }.toSeq
        constructor.newInstance(inputs: _*).asInstanceOf[T]
      }
      catch { case t:Throwable =>
        bug(s"Could not mirror $x")
        throw t
      }
    }
  }

  final protected def mirrorSym[A](sym: Sym[A]): Sym[A] = sym match {
    case Op(rhs) => mirror(sym,rhs)
    case _ => sym
  }

  final protected def removeSym(sym: Sym[_]): Unit = {
    state.scope = state.scope.filterNot(_ == sym)
    state.impure = state.impure.filterNot(_.sym == sym)
  }

  implicit class BlockOps[R](block: Block[R]) {
    def inline(): R = { val func = blockToFunction0(block, copy=true); func() }
    def toFunction0: () => R = blockToFunction0(block, copy=true)
  }
  implicit class Lambda1Ops[A,R](lambda1: Lambda1[A,R]) {
    def reapply(a: A): R = { val func = lambda1ToFunction1(lambda1, copy=true); func(a) }
    def toFunction1: A => R = lambda1ToFunction1(lambda1, copy=true)
  }
  implicit class Lambda2Ops[A,B,R](lambda2: Lambda2[A,B,R]) {
    def reapply(a: A, b: B): R = { val func = lambda2ToFunction2(lambda2, copy=true); func(a,b) }
    def toFunction2: (A, B) => R = lambda2ToFunction2(lambda2, copy=true)
  }
  implicit class Lambda3Ops[A,B,C,R](lambda3: Lambda3[A,B,C,R]) {
    def reapply(a: A, b: B, c: C): R = { val func = lambda3ToFunction3(lambda3, copy=true); func(a,b,c) }
    def toFunction3: (A, B, C) => R = lambda3ToFunction3(lambda3, copy=true)
  }

  protected def blockToFunction0[R](b: Block[R], copy: Boolean): () => R = () => inlineBlock(b).unbox
  protected def lambda1ToFunction1[A,R](b: Lambda1[A,R], copy: Boolean): A => R
  protected def lambda2ToFunction2[A,B,R](b: Lambda2[A,B,R], copy: Boolean): (A,B) => R
  protected def lambda3ToFunction3[A,B,C,R](b: Lambda3[A,B,C,R], copy: Boolean): (A,B,C) => R
}
