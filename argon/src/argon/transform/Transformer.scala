package argon
package transform

import argon.passes.Pass
import utils.tags.instrument

import scala.collection.mutable

/** An IR transformation pass.
  *
  * Transformers have two parts: a transform rule and a substitution method.
  *
  * The transform rule determines how a given (symbol, node) pair is altered.
  *
  * The substitution rule determines how transformed symbols are tracked.
  *
  * Several important terms:
  *   Copy   - Create a (recursive) duplicate of a value
  *   Mirror - Create a recursive copy of a value with substitutions
  *   Update - Modify the existing value with substitutions
  */
abstract class Transformer extends Pass {
  protected val f: Transformer = this

  /** Helper method for performing substitutions within pattern matching. */
  object F {
    /** Returns the substitution for the given value. */
    def unapply[T](x: T): Option[T] = Some(f(x))
  }


  def usedRemovedSymbol[T](x: T): Nothing = {
    dbgs(s"Used removed symbol $x!")
    throw new Exception(s"Used removed symbol: $x")
  }

  /** Returns the substitution x' for the given value x.
    * By default, this recursively mirrors most iterable collections, including Products.
    */
  def apply[T](x: T): T = {
    val y = (x match {
      case x: Mirrorable[_]    => mirrorMirrorable(x)
      case x: Sym[_]           => substituteSym(x)
      case x: Lambda1[a,_]     => substituteBlock(x).asLambda1[a]
      case x: Lambda2[a,b,_]   => substituteBlock(x).asLambda2[a,b]
      case x: Lambda3[a,b,c,_] => substituteBlock(x).asLambda3[a,b,c]
      case x: Block[_]         => substituteBlock(x)
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

    if (y.isInstanceOf[Invalid]) usedRemovedSymbol(x)
    y
  }

  /** Defines the substitution rule for a symbol s, i.e. the result of f(s). */
  protected def substituteSym[T](s: Sym[T]): Sym[T]

  /** Defines the substitution rule for a block b, i.e. the result of f(b). */
  protected def substituteBlock[T](b: Block[T]): Block[T]

  /** Perform the transformation t over all statements in block b as part of the current scope.
    * @return the transformed version of the block's result.
    */
  final protected def inlineWith[T](b: Block[T])(t: Seq[Sym[_]] => Sym[T]): Sym[T] = {
    state.logTab += 1
    val result = t(b.stms)
    state.logTab -= 1
    result
  }

  /** Perform some default transformation over all statements in block b as part of the current scope.
    * @return the transformed version of the block's result.
    */
  protected def inlineBlock[T](b: Block[T]): Sym[T]


  /** Explicitly transfer metadata from src to dest.
    *
    * Note that this will happen after staging and flow rules if called independently.
    * Use transferDataToAll to transfer as part of staging before flow rules.
    */
  final protected def transferData(src: Sym[_], dest: Sym[_]): Unit = {
    dest.name = src.name
    if (dest != src) {
      dest.prevNames = (state.paddedPass(state.pass - 1), s"$src") +: src.prevNames
    }

    metadata.all(src).toList.foreach{case (k,m) => m.transfer match {
      case Transfer.Mirror => metadata.add(dest, k, mirror(m))
      case Transfer.Remove => metadata.remove(dest, k)
      case Transfer.Ignore => // Do nothing
    }}
  }

  /** Transfers the metadata of src to all symbols created within the given scope. */
  final protected def transferDataToAll[R](src: Sym[_])(scope: => R): R = {
    withFlow(s"transferDataToAll_$src", {dest => transferData(src, dest) }, prepend = true)(scope)
  }

  /** Transfers the metadata of src to dest if dest is "newer" than src.
    *
    * Note that this happens after staging/flows. Use transferDataToAllNew for a flow rule version.
    */
  final protected def transferDataIfNew(src: Sym[_], dest: Sym[_]): Unit = {
    // TODO[5]: Better way to determine "newness" of dest?
    val shouldTransfer = (src.rhs.getID,dest.rhs.getID) match {
      case (Some(id1),Some(id2)) => id1 <= id2
      case _ => true
    }
    if (shouldTransfer) {
      transferData(src, dest)
    }
  }

  /** Transfers the metadata of src to all new symbols created within the given scope. */
  final protected def transferDataToAllNew[R](src: Sym[_])(scope: => R): R = {
    withFlow(s"transferDataToAllNew_$src", {dest => transferDataIfNew(src, dest) }, prepend = true)(scope)
  }



  private[argon] def mirror[M](m: Data[M]): Data[M] = m.mirror(f).asInstanceOf[Data[M]]


  final protected def mirrorSym[A](sym: Sym[A]): Sym[A] = sym match {
    case Op(rhs) => mirror(sym,rhs)
    case _ => sym
  }

  final def mirror[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    implicit val tA: Type[A] = rhs.R
    implicit val ctx: SrcCtx = lhs.ctx
    try {
      tA.boxed(stageWithFlow( mirrorNode(rhs) ){lhs2 => transferDataIfNew(lhs,lhs2) })
    }
    catch {case t: Throwable =>
      bug(s"An error occurred while mirroring $lhs = $rhs")
      throw t
    }
  }

  protected def mirrorNode[A](rhs: Op[A]): Op[A] = rhs.mirror(f)

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

  /** Explicitly removes a symbol from the scope currently being staged.
    * Does nothing for symbols defined outside this scope.
    */
  final protected def removeSym(sym: Sym[_]): Unit = {
    state.scope = state.scope.filterNot(_ == sym)
    state.impure = state.impure.filterNot(_.sym == sym)
  }

  implicit class BlockOps[R](block: Block[R]) {
    /** Inline all statements in the block into the current scope. */
    def inline(): R = { val func = blockToFunction0(block); func() }
    /** Turn this block into a callable Function0, where each call will inline the block. */
    def toFunction0: () => R = blockToFunction0(block)
  }
  implicit class Lambda1Ops[A,R](lambda1: Lambda1[A,R]) {
    /** Inline all statements in the block into the current scope with the given input. */
    def reapply(a: A): R = { val func = lambda1ToFunction1(lambda1); func(a) }
    /** Turn this block into a callable Function1, where each call will reapply the Lambda. */
    def toFunction1: A => R = lambda1ToFunction1(lambda1)
  }
  implicit class Lambda2Ops[A,B,R](lambda2: Lambda2[A,B,R]) {
    /** Inline all statements in the block into the current scope with the given inputs. */
    def reapply(a: A, b: B): R = {
      dbgs(s"Making Function2")
      val func = lambda2ToFunction2(lambda2)
      dbgs(s"Executing Function2 with inputs: $a and $b")
      func(a,b)
    }
    def toFunction2: (A, B) => R = lambda2ToFunction2(lambda2)
  }
  implicit class Lambda3Ops[A,B,C,R](lambda3: Lambda3[A,B,C,R]) {
    def reapply(a: A, b: B, c: C): R = { val func = lambda3ToFunction3(lambda3); func(a,b,c) }
    def toFunction3: (A, B, C) => R = lambda3ToFunction3(lambda3)
  }

  protected def blockToFunction0[R](b: Block[R]): () => R = () => inlineBlock(b).unbox
  protected def lambda1ToFunction1[A,R](b: Lambda1[A,R]): A => R
  protected def lambda2ToFunction2[A,B,R](b: Lambda2[A,B,R]): (A,B) => R
  protected def lambda3ToFunction3[A,B,C,R](b: Lambda3[A,B,C,R]): (A,B,C) => R


  /** Called before the top-level block is traversed. */
  override protected def preprocess[R](block: Block[R]): Block[R] = {
    state.cache = Map.empty              // Clear CSE cache prior to transforming
    globals.invalidateBeforeTransform()  // Reset unstable global metadata
    block
  }

}
