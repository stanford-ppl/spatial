package spatial.traversal

import argon._
import argon.passes.Traversal
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

  /** True if symbol x is known to be constant for the duration of all iterations of i.
    * - True when this value is constant
    * - True when this value is a register read, with the register written outside this loop
    * - Otherwise true if this symbol is defined OUTSIDE of the scope of this iterator
    */
  private def isInvariant(i: Idx, x: Sym[_]): Boolean = x match {
    case Expect(_) => true
    case Op(RegRead(reg)) =>
      val loop = loops(i)
      reg.writers.forall{writer => LCA(writer.parent,x.parent) != loop.toCtrl }
    case _ => !scopes(i).contains(x)
  }

  /** True if all symbols in xs are invariant to all iterators in is. */
  private def isAllInvariant(is: Seq[Idx], xs: Seq[Sym[_]]): Boolean = {
    xs.forall{x => is.forall{i => isInvariant(i,x) }}
  }

  /** Returns the innermost iterator which the symbols in xs vary with.
    * If x is entirely loop invariant, returns None.
    */
  private def lastVariantIter(is: Seq[Idx], x: Sym[_]): Option[Idx] = {
    is.reverseIterator.find{i => !isInvariant(i,x) }
  }

  object Plus  { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{case FixAdd(a,b) => (a,b) }}
  object Minus { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{case FixSub(a,b) => (a,b) }}
  object Times { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{case FixMul(a,b) => (a,b) }}
  object Index { def unapply[W](x: Ind[W]): Option[Ind[W]] = Some(x).filter(iters.contains) }

  private lazy val Zero = Sum.single(0)
  private lazy val One  = Prod.single(1)
  private def stride[W](i: Ind[W]): Prod = if (i.getCounter.isDefined) Prod.single(i.ctrStep) else One

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

  private def makeAddressPattern(is: Seq[Idx], components: Seq[AffineProduct], offset: Sum): AddressPattern = {
    val lastIters = offset.syms.mapping{x => lastVariantIter(is,x) } ++
                    components.flatMap{prod => prod.syms.mapping{x => lastVariantIter(is,x) }}

    val lastIter  = lastIters.values.maxByOrElse(None){i => i.map{is.indexOf}.getOrElse(-1) }

    AddressPattern(components, offset, lastIters, lastIter)
  }

  /** Return the affine access pattern of the given address component x as an AddressPattern.
    * @param mem the memory being accessed
    * @param access the symbol of the memory access
    * @param x a single dimension of the (potentially multi-dimensional) access address
    */
  private def getAccessAddressPattern(mem: Sym[_], access: Sym[_], x: Idx): AddressPattern = {
    val Affine(products, offset) = x
    val components = products.toAffineProducts
    val is = accessIterators(access, mem)
    makeAddressPattern(is, components, offset)
  }

  /** Return the affine pattern of the given value x as an AddressPattern.
    * Not intended for use with memory access addresses.
    * For general use in discovering access patterns in general integer values.
    */
  private def getValueAddressPattern(x: Idx): AddressPattern = {
    val Affine(products, offset) = x
    val components = products.toAffineProducts
    makeAddressPattern(iters, components, offset)
  }

  /** Spoof an address pattern for a streaming access to a memory (e.g. fifo push, regfile shift)
    * and return the result as an AddressPattern.
    * This spoofing is done by treating the streaming access as a linear access and is used so that
    * streaming and addressed accesses can be analyzed using the same affine pattern logic.
    */
  private def setAccessPattern(mem: Sym[_], access: Sym[_], addr: Seq[Idx]): Unit = {
    dbgs(s"${stm(access)}")

    val pattern = addr.map{x => getAccessAddressPattern(mem, access, x) }

    dbgs(s"  Access pattern: ")
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"  [$d] $p") }

    val matrices = getUnrolledMatrices(mem,access,addr,pattern,Nil)
    access.accessPattern = pattern
    access.affineMatrices = matrices

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
    dbgs(s"${stm(access)} [STREAMING]")

    val is = accessIterators(access, mem)
    val ps = is.map(_.ctrPar.toInt)
    val as = Array.tabulate(is.length){d => ps.drop(d+1).product }
    val offset = Sum.single(0)
    val components = as.zip(is).map{case (a,i) => AffineProduct(Sum.single(a),i) }
    val ap = makeAddressPattern(is, components, offset)

    val pattern = Seq(ap)

    dbgs(s"  Access pattern: ")
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"  [$d] $p") }

    val matrices = getUnrolledMatrices(mem, access, Nil, pattern, Nil)
    access.accessPattern = pattern
    access.affineMatrices = matrices

    dbgs(s"  Access matrices: ")
    matrices.foreach{m => dbgss(m) }
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = lhs match {
    case Op(op@CounterNew(start,end,step,_)) if op.A.isIdx =>
      start.accessPattern = Seq(getValueAddressPattern(start.asInstanceOf[Idx]))
      end.accessPattern   = Seq(getValueAddressPattern(end.asInstanceOf[Idx]))
      step.accessPattern  = Seq(getValueAddressPattern(step.asInstanceOf[Idx]))

    case Op(loop: Loop[_]) =>
      loop.bodies.foreach{case (is,blocks) =>
        dbgs(s"$lhs = $rhs [LOOP]")
        dbgs(s"  Blocks: $blocks")
        dbgs(s"  Iterators: $is")
        inLoop(lhs, is, blocks)
      }

    case Dequeuer(mem,adr,_)   if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Enqueuer(mem,_,adr,_) if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Reader(mem,adr,_)   => setAccessPattern(mem, lhs, adr)
    case Writer(mem,_,adr,_) => setAccessPattern(mem, lhs, adr)
    case _ => super.visit(lhs, rhs)
  }
}
