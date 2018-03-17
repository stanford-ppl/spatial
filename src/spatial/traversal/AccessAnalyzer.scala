package spatial.traversal

import core._
import core.passes.Traversal
import utils.implicits.collections._

import spatial.lang._
import spatial.data._
import spatial.node._
import spatial.util._

case class AccessAnalyzer(IR: State) extends Traversal with AccessExpansion {
  private var iters: Seq[Idx] = Nil                     // List of loop iterators, ordered outermost to innermost
  private var loops: Map[Idx,Sym[_]] = Map.empty        // Map of loop iterators to defining loop symbol
  private var scopes: Map[Idx,Set[Sym[_]]] = Map.empty  // Map of looop iterators to all symbols defined in that scope

  private def inLoop(loop: Sym[_], is: Seq[Idx], blocks: Seq[Block[_]]): Unit = {
    val saveIters = iters
    val saveLoops = loops
    val saveScopes = scopes

    val scope = blocks.flatMap(_.nestedStms).toSet
    iters ++= is
    loops ++= is.map{_ -> loop}
    scopes ++= is.map{_ -> scope}
    blocks.foreach{b => visitBlock(b) }

    iters = saveIters
    loops = saveLoops
    scopes = saveScopes
  }

  /**
    * Returns true if symbol x is known to be constant for the duration
    * of all iterations of i.
    * - True when this value is constant
    * - True when this value is a register read, with the register written outside this loop
    * - Otherwise true if this symbol is defined OUTSIDE of the scope of this iterator
    */
  private def isInvariant(i: Idx, x: Sym[_]): Boolean = x match {
    case Expect(_) => true
    case Op(RegRead(reg)) =>
      val loop = loops(i)
      writersOf(reg).forall{writer => LCA(writer.parent,x.parent) != loop.toCtrl }
    case _ => !scopes(i).contains(x)
  }

  /**
    * Returns true if all symbols in xs are invariant to all iterators in is.
    */
  private def isAllInvariant(is: Seq[Idx], xs: Seq[Sym[_]]): Boolean = {
    xs.forall{x => is.forall{i => isInvariant(i,x) }}
  }

  /**
    * Returns the innermost iterator which the symbols in xs vary with.
    */
  private def lastVariantIter(is: Seq[Idx], x: Sym[_]): Option[Idx] = {
    is.reverseIterator.find{i => !isInvariant(i,x) }
  }

  object Plus  { def unapply[W](x: I[W]): Option[(I[W],I[W])] = x.op.collect{case FixAdd(a,b) => (a,b) }}
  object Minus { def unapply[W](x: I[W]): Option[(I[W],I[W])] = x.op.collect{case FixSub(a,b) => (a,b) }}
  object Times { def unapply[W](x: I[W]): Option[(I[W],I[W])] = x.op.collect{case FixMul(a,b) => (a,b) }}
  object Index { def unapply[W](x: I[W]): Option[I[W]] = Some(x).filter(iters.contains) }

  private lazy val Zero = Sum.single(0)
  private lazy val One  = Prod.single(1)
  private def stride[W](i: I[W]): Prod = if (ctrOf.get(i).isDefined) Prod.single(i.ctrStep) else One

  implicit class AffineComponents(x: Seq[AffineComponent]) {
    def unary_-(): Seq[AffineComponent] = x.map{c => -c}

    def inds: Seq[Idx] = x.map(_.i)
    def *(a: Sum): Seq[AffineComponent] = x.flatMap(_ * a)

    /**
      * Converts a representation of the form
      * a10*i0 + ... + a1N0*i0 + aM0*iM + ... aMNM*iM to
      * (a10 + ... + a1N0)*i0 + ... + (aM0 + ... aMNM)*iM
      */
    def toAffineProducts: Seq[AffineProduct] = {
      x.groupBy(_.i).toList
       .sortBy{case (i,_) => iters.indexOf(i) }
       .map{case (i,comps) => AffineProduct(comps.map(_.a).foldLeft(Zero){_+_},i) }
    }
  }

  private object Offset {
    def unapply(x: Idx): Option[Sum] = x match {
      case Affine(a,b) if a.isEmpty => Some(b)
      case _ => None
    }
  }

  private object Affine {
    def unapply(x: Idx): Option[(Seq[AffineComponent], Sum)] = x match {
      case Index(i) => Some(Seq(AffineComponent(stride(i), i)), Zero)

      case Plus(Affine(a1,b1), Affine(a2,b2))  => Some(a1 ++ a2, b1 + b2)
      case Minus(Affine(a1,b1), Affine(a2,b2)) => Some(a1 ++ (-a2), b1 - b2)

      case Times(Affine(a,b1), Offset(b2)) if isAllInvariant(a.inds, b2.syms) => Some(a * b2, b1 * b2)
      case Times(Offset(b1), Affine(a,b2)) if isAllInvariant(a.inds, b1.syms) => Some(a * b1, b1 * b2)

      case s => Some(Nil, Sum.single(s))
    }
  }

  private def getAddressPattern(x: Idx): AddressPattern = {
    val Affine(products, offset) = x
    val components = products.toAffineProducts
    val lastIters  = offset.syms.mapping{x => lastVariantIter(iters,x) }
    val lastIter   = lastIters.values.maxByOrElse(None){i => i.map{iters.indexOf}.getOrElse(-1) }
    AddressPattern(components, offset, lastIters, lastIter)
  }

  private def setAccessPattern(mem: Sym[_], access: Sym[_], addr: Seq[Idx]): Unit = {
    val pattern = addr.map(getAddressPattern)
    val matrices = getUnrolledMatrices(mem,access,addr,pattern,Nil)
    accessPatternOf(access) = pattern
    affineMatricesOf(access) = matrices

    dbgs(s"${stm(access)}")
    dbgs(s"  Access pattern: ")
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"  [$d] $p") }
    dbgs(s"  Access matrices: ")
    matrices.foreach{m => dbgss(m) }
  }

  /**
    * Fake the access pattern of a streaming access (enqueue or dequeue)
    * as a N-D sum of all iterators between the access and the memory.
    *
    * Foreach(0 until N par 2, 0 until M par 8){(i,j) =>
    *   x.enq(...)
    * }
    * will have access pattern (8*i + j)
    */
  private def setStreamingPattern(mem: Sym[_], access: Sym[_]): Unit = {
    val is = accessIterators(access, mem)
    val ps = is.map(_.ctrPar.toInt)
    val as = Array.tabulate(is.length){d => ps.drop(d+1).product }
    val components = as.zip(is).map{case (a,i) => AffineProduct(Sum.single(a),i) }
    val ap = AddressPattern(components, Sum.single(0), Map.empty, is.lastOption)

    val pattern = Seq(ap)
    val matrices = getUnrolledMatrices(mem, access, Nil, pattern, Nil)
    accessPatternOf(access) = pattern
    affineMatricesOf(access) = matrices

    dbgs(s"${stm(access)} [STREAMING]")
    dbgs(s"  Access pattern: ")
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"  [$d] $p") }
    dbgs(s"  Access matrices: ")
    matrices.foreach{m => dbgss(m) }
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = lhs match {
    case Op(op@CounterNew(start,end,step,_)) if op.A.isIdx =>
      accessPatternOf(start) = Seq(getAddressPattern(start.asInstanceOf[Idx]))
      accessPatternOf(end)   = Seq(getAddressPattern(end.asInstanceOf[Idx]))
      accessPatternOf(step)  = Seq(getAddressPattern(step.asInstanceOf[Idx]))

    case Op(loop: Loop[_]) =>
      loop.bodies.foreach{case (is,blocks) => inLoop(lhs, is, blocks) }

    case Dequeuer(mem,adr,_)   if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Enqueuer(mem,_,adr,_) if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Reader(mem,adr,_)   => setAccessPattern(mem, lhs, adr)
    case Writer(mem,_,adr,_) => setAccessPattern(mem, lhs, adr)
    case _ => super.visit(lhs, rhs)
  }
}
