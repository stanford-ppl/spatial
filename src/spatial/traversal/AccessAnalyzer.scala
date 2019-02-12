package spatial.traversal

import argon._
import argon.node._
import argon.passes.Traversal
import utils.implicits.collections._

import spatial.lang._
import spatial.node._
import spatial.util.modeling
import spatial.metadata.access._
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._

case class AccessAnalyzer(IR: State) extends Traversal with AccessExpansion {
  private var iters: Seq[Idx] = Nil                     // List of loop iterators, ordered outermost to innermost
  private var iterStarts: Map[Idx, Ind[_]] = Map.empty  // Map from loop iterator to its respective start value
  private var loops: Map[Idx,Sym[_]] = Map.empty        // Map of loop iterators to defining loop symbol
  private var scopes: Map[Idx,Set[Sym[_]]] = Map.empty  // Map of loop iterators to all symbols defined in that scope
  private var mostRecentWrite: Map[Reg[_], Sym[_]] = Map.empty

  private def inLoop(loop: Sym[_], is: Seq[Idx], istarts: Seq[Ind[_]], block: Block[_]): Unit = {
    val saveIters = iters
    val saveIterStarts = iterStarts
    val saveLoops = loops
    val saveScopes = scopes

    val scope = block.nestedStms.toSet
    iters ++= is
    iterStarts ++= is.indices.map{i => is(i) -> istarts(i)}
    loops ++= is.map{_ -> loop}
    scopes ++= is.map{i => (i -> modeling.consumersDfs(i.consumers,Set(), scope)) }
    visitBlock(block)

    iters = saveIters
    iterStarts = saveIterStarts
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
    case _ => dbgs(s"isInvariant $i, $x?  check against ${scopes(i)}");!scopes(i).contains(x)
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

  object Plus  { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{case FixAdd(LU(a),LU(b)) => (a,b) }}
  object Minus { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{case FixSub(LU(a),LU(b)) => (a,b) }}
  object Times { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{
    case FixMul(LU(a),LU(b)) => (a,b)
    case FixSLA(LU(a),Expect(b)) => (a,a.from(scala.math.pow(2,b)))
  }}
  object Divide { def unapply[W](x: Ind[W]): Option[(Ind[W],Ind[W])] = x.op.collect{
    case FixDiv(LU(a),LU(b)) => (a,b)
    case FixSRA(LU(a),Expect(b)) => (a,a.from(scala.math.pow(2,b)))
  }}
  object Mod   { def unapply[W](x: Ind[W]): Option[(Ind[W], Int)] = x.op.collect{case FixMod(a,b) if b.isConst => (a,b.toInt)}}
  object Index { def unapply[W](x: Ind[W]): Option[Ind[W]] = Some(x).filter(iters.contains) }
  object LU    { def unapply[W](x: Ind[W]): Option[Ind[W]] = Some(x.op.collect{case RegRead(reg) if mostRecentWrite.contains(reg) => mostRecentWrite(reg).asInstanceOf[Ind[W]]}.getOrElse(x))}
  object Read  { def unapply[W](x: Ind[W]): Option[Ind[W]] = x.op.collect{case RegRead(reg) if mostRecentWrite.contains(reg) => mostRecentWrite(reg).asInstanceOf[Ind[W]]}}

  private lazy val Zero = Sum.single(0)
  private lazy val One  = Prod.single(1)
  private lazy val NotSet = Modulus.notset
  private def stride[W](i: Ind[W]): Prod = if (i.getCounter.isDefined) Prod.single(i.ctrStep) else One
  private def isNullIndex[W](i: Ind[W]): Boolean = if (i.getCounter.isDefined && i.ctrEnd.isConst && i.ctrStart.isConst) {i.ctrEnd.toInt == 1 && i.ctrStart.toInt == 0} else false

  implicit class AffineComponents(x: Seq[AffineComponent]) {
    def unary_-(): Seq[AffineComponent] = x.map{c => -c}

    def inds: Seq[Idx] = x.map(_.i)
    def *(s: Sum): Seq[AffineComponent] = x.flatMap(_ * s)
    def /(s: Sum): Seq[AffineComponent] = x.map(_ / s)

    def canBeDividedBy(s: Sum): Boolean = x.forall(_.canBeDividedBy(s))

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
    /** Returns only affine patterns which have no loop iterators. */
    def unapply(x: Idx): Option[Sum] = x match {
      case Affine(a,b,m) if a.isEmpty => Some(b)
      case _ => None
    }
  }

  def combine(a1: Seq[AffineComponent], a2: Seq[AffineComponent]): Seq[AffineComponent] = {
    val combined = a1 ++ a2
    var remainingIndices = Seq.tabulate(combined.size){i => i}.toSet
    combined.zipWithIndex.foreach{
      case (a,i) if (remainingIndices.contains(i)) => 
        val matches = combined.zipWithIndex.collect{case (x,j) if (-a == x) => j}.filter(remainingIndices.contains)
        if (matches.nonEmpty) {
          remainingIndices -= matches.head
          remainingIndices -= i
        }
      case _ => 
    }
    remainingIndices.toSeq.sorted.map(combined)
  }

  private object Affine {
    /** Recursively finds affine patterns in the dataflow graph starting from the given symbol x.
      * Examples of patterns:
      *   3       ==> Nil, Sum(3)
      *   x       ==> Nil, Sum(x) (if x is not a loop iterator or defined by an affine function)
      *   i*3 + j ==> Seq(AffineComponent(Prod(3), i), AffineComponent(Prod(1), j)), Sum(0)
      *   i*x*y   ==> Seq(AffineComponent(Prod(x, y), i)), Sum(0)
      *
      * Modulus is always unset for now.
      */
    def unapply(x: Idx): Option[(Seq[AffineComponent], Sum, Modulus)] = x match {
      // A single loop iterator
      case Index(i) if !isNullIndex(i) => Some(Seq(AffineComponent(stride(i), i)), Zero, NotSet)
      case Index(i) if isNullIndex(i)  => Some(Nil, Zero, NotSet)

      // Any affine component plus any other affine component (e.g. (4*i + 5) + (3*j + 2))
      case Plus(Affine(a1,b1,m1), Affine(a2,b2,m2)) if !m1.set && !m2.set  => Some(combine(a1, a2), b1 + b2, NotSet)

      // Any affine component minus any other affine component (e.g. j - 1)
      case Minus(Affine(a1,b1,m1), Affine(a2,b2,m2)) if !m1.set && !m2.set => Some(combine(a1, -a2), b1 - b2, NotSet)

      // Product of an affine component with a loop independent value, e.g. 4*i or (i + j)*32
      // Note: the multiplier only has to be loop invariant w.r.t. to iterators in a.
      // Note: products of affine components (e.g. i*j) are NOT themselves affine
      // Multiplication on sequences of AffineComponents is implicitly defined to be distributive
      case Times(Affine(a,b1,m), Offset(b2)) if isAllInvariant(a.inds, b2.syms) && !m.set => Some(a * b2, b1 * b2, NotSet)
      case Times(Offset(b1), Affine(a,b2,m)) if isAllInvariant(a.inds, b1.syms) && !m.set => Some(a * b1, b1 * b2, NotSet)

      //case Mod(Affine(a,b,m1), m2) => Some(a, b, m1 % Modulus(m2))

      case Read(Affine(a,b,m)) => Some(a, b, m)

      //case Divide(Affine(a,b1,m), Offset(b2)) if isAllInvariant(a.inds, b2.syms) && a.canBeDividedBy(b2) && b1.canBeDividedBy(b2) =>
      //  Some(a / b2, b1 / b2, m)

      case s => Some(Nil, Sum.single(s), NotSet)
    }
  }

  private def makeAddressPattern(is: Seq[Idx], components: Seq[AffineProduct], offset: Sum, modulus: Modulus): AddressPattern = {
    val starts = iterStarts.filterKeys(is.filter{i => offset.syms.contains(i) || components.exists{prod => prod.syms.contains(i)}}.contains)
    val lastIters = offset.syms.mapping{x => lastVariantIter(is,x) } ++
                    components.flatMap{prod => prod.syms.mapping{x => lastVariantIter(is,x) }} ++
                    starts.values.mapping{x => lastVariantIter(is,x)}

    val lastIter  = lastIters.values.maxByOrElse(None){i => i.map{is.indexOf}.getOrElse(-1) }
    AddressPattern(components, offset, lastIters, lastIter, starts)
  }

  /** Return the affine access pattern of the given address component x as an AddressPattern.
    * @param mem the memory being accessed
    * @param access the symbol of the memory access
    * @param x a single dimension of the (potentially multi-dimensional) access address
    */
  private def getAccessAddressPattern(mem: Sym[_], access: Sym[_], x: Idx): AddressPattern = {
    val Affine(products, offset, modulus) = x
    val components = products.toAffineProducts
    val is = accessIterators(access, mem)
    makeAddressPattern(is, components, offset, modulus)
  }

  /** Return the affine pattern of the given value x as an AddressPattern.
    * Not intended for use with memory access addresses.
    * For general use in discovering access patterns in general integer values.
    */
  private def getValueAddressPattern(x: Idx): AddressPattern = {
    val Affine(products, offset, modulus) = x
    val components = products.toAffineProducts
    makeAddressPattern(iters, components, offset, NotSet)
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
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"    [$d] $p") }

    val matrices = getUnrolledMatrices(mem,access,addr,pattern,Nil)
    access.accessPattern = pattern
    access.affineMatrices = matrices

    dbgs(s"  Unrolled matrices: ")
    matrices.foreach{m => dbgss("    ", m) }
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
    val ap = makeAddressPattern(is, components, offset, NotSet)

    val pattern = Seq(ap)

    dbgs(s"  Access pattern: ")
    pattern.zipWithIndex.foreach{case (p,d) => dbgs(s"  [$d] $p") }

    val matrices = getUnrolledMatrices(mem, access, Nil, pattern, Nil)
    access.accessPattern = pattern
    access.affineMatrices = matrices

    dbgs(s"  Unrolled matrices: ")
    matrices.foreach{m => dbgss("    ", m) }
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = lhs match {
    case Op(op@CounterNew(start,end,step,_)) if op.A.isIdx =>
      dbgs(s"$lhs = $rhs [COUNTER]")
      start.accessPattern = Seq(getValueAddressPattern(start.asInstanceOf[Idx]))
      end.accessPattern   = Seq(getValueAddressPattern(end.asInstanceOf[Idx]))
      step.accessPattern  = Seq(getValueAddressPattern(step.asInstanceOf[Idx]))
      dbgs(s"  start: ${start.accessPattern}")
      dbgs(s"  end: ${end.accessPattern}")
      dbgs(s"  step: ${step.accessPattern}")

    case Op(loop: Loop[_]) if loop.cchains.forall(!_._1.isForever) =>
      loop.bodies.foreach{scope =>
        dbgs(s"$lhs = $rhs [LOOP]")
        scope.blocks.foreach{case (iters, block) =>
          val iterStarts = iters.map(_.ctrStart)
          dbgs(s"  Iters:  $iters")
          dbgs(s"  Starts:  $iterStarts")
          dbgs(s"  Blocks: $block")
          inLoop(lhs, iters, iterStarts, block)
        }
      }

    case Op(loop: Loop[_]) =>
      loop.bodies.foreach{scope =>
        dbgs(s"$lhs = $rhs [LOOP]")
        scope.blocks.foreach{case (iters, block) =>
          val iterStarts = iters.map{_ => I32(0)}
          dbgs(s"  Iters:  $iters")
          dbgs(s"  Starts:  $iterStarts")
          dbgs(s"  Blocks: $block")
          inLoop(lhs, iters, iterStarts, block)
        }
      }

    case Dequeuer(mem,adr,_)   if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Enqueuer(mem,_,adr,_) if adr.isEmpty => setStreamingPattern(mem, lhs)
    case Reader(mem,adr,_)   => 
      lhs match {
        case Op(RegRead(reg)) if !reg.isRemoteMem => 
          val reachingWrite = reachingWritesToReg(lhs, reg.writers.toSet)
          if (reachingWrite.size == 1 && reachingWrite.head.accumType == AccumType.Unknown) {
            val data = reachingWrite.head match {
              case Op(x: Enqueuer[_]) => x.data
              case _ => throw new Exception(s"Unexpected write to $reg: ${reachingWrite.head}")
            }
            dbgs(s"Creating reaching write subst rule for $lhs = $data")
            mostRecentWrite += (reg -> data)
          }
        case _ =>
      }
      setAccessPattern(mem, lhs, adr)
    case Writer(mem,_,adr,_) => 
      setAccessPattern(mem, lhs, adr)
    case _ => super.visit(lhs, rhs)
  }
}
