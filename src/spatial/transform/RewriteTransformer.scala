package spatial.transform

import argon._
import argon.node._
import emul.ResidualGenerator._
import argon.transform.MutateTransformer
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.metadata.rewrites._
import spatial.metadata.retiming._
import spatial.metadata.math._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

import utils.math.{isPow2,log2}

/** Performs hardware-specific rewrite rules. */
case class RewriteTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  def selectMod[S,I,F](x: FixPt[S,I,F], y: Int): FixPt[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    if (log2(y.toDouble) == 0) x.from(0)
    // TODO: Consider making a node like case class BitRemap(data: Bits[_], remap: Seq[Int], outType: Bits[T]) that wouldn't add to retime latency
    else x & (scala.math.pow(2,log2(y.toDouble))-1).to[Fix[S,I,F]] 
  }

  def constMod[S,I,F](x: FixPt[S,I,F], y: Seq[Int]): FixPt[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    if (y.size==1) {
      x.from(y.head)
    } else {
      val b = boundVar[FixPt[S,I,F]]
      b.vecConst = y
      b
    }
  }

  def writeReg[A](lhs: Sym[_], reg: Reg[_], data: Bits[A], ens: Set[Bit]): Void = {
    implicit val A: Bits[A] = data.selfType
    stageWithFlow( RegWrite[A](reg.asInstanceOf[Reg[A]], data, ens) ){lhs2 => transferData(lhs, lhs2) }
  }

  def fltFMA[M,E](m1: Flt[M,E], m2: Flt[M,E], add: Flt[M,E]): Flt[M,E] = {
    implicit val M: INT[M] = add.fmt.m
    implicit val E: INT[E] = add.fmt.e
    stage(FltFMA(m1,m2,add))
  }

  def fixFMA[S,I,F](m1: Fix[S,I,F], m2: Fix[S,I,F], add: Fix[S,I,F]): Fix[S,I,F] = {
    implicit val S: BOOL[S] = add.fmt.s
    implicit val I: INT[I] = add.fmt.i
    implicit val F: INT[F] = add.fmt.f
    stage(FixFMA(m1,m2,add))
  }

  def fltRecipSqrt[M,E](x: Flt[M,E]): Flt[M,E] = {
    implicit val M: INT[M] = x.fmt.m
    implicit val E: INT[E] = x.fmt.e
    stage(FltRecipSqrt(x))
  }

  def fixRecipSqrt[S,I,F](x: Fix[S,I,F]): Fix[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    stage(FixRecipSqrt(x))
  }

  def fixShiftCombine[S,I,F](x: Fix[S,I,F], y: Int, dir: String): Fix[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    dir match {
      case _ if y == 0 => x
      case "sla" if y > 0 => stage(FixSLA(x,y))
      case "sla" if y < 0 => stage(FixSRA(x,-y))
      case "sra" if y > 0 => stage(FixSRA(x,y))
      case _ /*"sra" if y < 0*/ => stage(FixSLA(x,-y))
    }
  }

  def static(lin: scala.Int, iter: Num[_], y: scala.Int): Boolean = {
    def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
    if (iter.counter.ctr.isStaticStartAndStep) {
      val Final(step) = iter.counter.ctr.step
      val par = iter.counter.ctr.ctrPar.toInt
      val g = gcd(par*step*lin,y)
      if (g == 1) false // Residue set of lin % y is {0,1,...,y-1}
      else if (g % y == 0) true // Residue set of lin % y is {0}
      else false // Residue set of lin % y is {0, g, 2g, ..., y-g}
    }
    else false
  }

  def residual(lin: scala.Int, iter: Num[_], ofs: scala.Int, y: scala.Int): ResidualGenerator = {
    def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
    if (iter.getCounter.isDefined && iter.counter.ctr.isStaticStartAndStep) {
      val Final(start) = iter.counter.ctr.start
      val Final(step) = iter.counter.ctr.step
      val par = iter.counter.ctr.ctrPar.toInt
      val lanes = iter.counter.lanes
      val A = gcd(par * step * lin, y)
      val B = lanes.map { lane => (((start + ofs + lane * step * lin) % y) + y) % y }
      dbgs(s"Residual Generator for lane $lanes with step $step, lin $lin and start $start + $ofs under mod $y = $A, $B")
      ResidualGenerator(A, B, y)
    }
    else ResidualGenerator(1, 0, y)
  }

  def getPosMod(lin: scala.Int, x: Num[_], ofs: scala.Int, mod: scala.Int) = {
    val res = residual(lin, x, ofs, mod)
    res.resolvesTo.get
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }

    case RegWrite(F(reg), F(data), F(en)) => data match {
      // Look for very specific FixFMA pattern and replace with FixFMAAccum node (Issue #63).. This gives error about not wanting to match Node[Fix[S,I,F]] <: Node[Any] because Op is type R and not +R
      // case Op(Mux(
      //             F(Op(FixEql(a,b))),
      //             F(Op(FixMul(mul1: Fix[s,i,f], mul2))),
      //             F(Op(FixFMA(mul11,mul22,Op(RegRead(`reg`)))))
      //         )) => 
      //   // fixFMAAccum(mul1,mul2).asInstanceOf[Sym[A]] // Assumes the FixEql is just flagging the first iter, which can be done in template
      //   super.transform(lhs,rhs)

      // Change a write from a mux with the register or some other value to an enabled register write
      case Op( Mux(sel, Op(RegRead(`reg`)), b) ) =>
        val lhs2 = writeReg(lhs, reg, b, en + !sel)
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      // Change a write from a mux with the register or some other value to an enabled register write
      case Op( Mux(sel, a, Op(RegRead(`reg`))) ) =>
        val lhs2 = writeReg(lhs, reg, a, en + sel)
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      case _ => super.transform(lhs, rhs)
    }

    case FixMod(F(x), F(Final(y))) if inHw => 
      x match {
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixAdd(F(xx), Final(ofs))) => 
          if (xx.getCounter.isDefined && static(1, xx, y)) {
            val posMod = getPosMod(1, xx, ofs, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(1, xx, ofs, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(1, xx, ofs, y)
            m
          }
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixAdd(Final(ofs), F(xx))) => 
          if (xx.getCounter.isDefined && static(1, xx, y)) {
            val posMod = getPosMod(1, xx, ofs, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(1, xx, ofs, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(1, xx, ofs, y)
            m
          }
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixSub(F(xx), Final(ofs))) => 
          if (xx.getCounter.isDefined && static(1, xx, y)) {
            val posMod = getPosMod(1, xx, -ofs, y)  
            transferDataToAllNew(lhs){ constMod(x, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(1, xx, -ofs, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(1, xx, -ofs, y)
            m
          }
        case Op(FixMul(Final(lin), F(xx))) => 
          if (xx.getCounter.isDefined && static(lin, xx, y)) {
            val posMod = getPosMod(lin, xx, 0, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(lin, xx, 0, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(lin, xx, 0, y)
            m
          }
        case Op(FixMul(F(xx), Final(lin))) => 
          if (xx.getCounter.isDefined && static(lin, xx, y)) {
            val posMod = getPosMod(lin, xx, 0, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(lin, xx, 0, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(lin, xx, 0, y)
            m
          }
        case Op(FixFMA(Final(lin), F(xx), Final(ofs))) => 
          if (xx.getCounter.isDefined && static(lin, xx, y)) {
            val posMod = getPosMod(lin, xx, ofs, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(lin, xx, ofs, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(lin, xx, ofs, y)
            m
          }
        case Op(FixFMA(F(xx), Final(lin), Final(ofs))) => 
          if (xx.getCounter.isDefined && static(lin, xx, y)) {
            val posMod = getPosMod(lin, xx, ofs, y)  
            transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(lin, xx, ofs, y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = residual(lin, xx, ofs, y)
            m
          }
        case _ =>
          if (x.getCounter.isDefined && static(1, x, y)) {
            val posMod = getPosMod(1, x, 0, y)  
            transferDataToAllNew(lhs){ constMod(x, posMod).asInstanceOf[Sym[A]] }
          } else if (x.getCounter.isDefined && isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = residual(1, x, 0, y)
            m     
          } else if (isPow2(y)) {
            val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
            dbgs(s"Cannot statically determine $x % $y")
            m.modulus = y
            m.residual = ResidualGenerator(1,0,y)
            m     
          } else {
            val m = super.transform(lhs,rhs)
            m.residual = ResidualGenerator(1,0,y)
            m
          }
      }

    // 1 / sqrt(b)  ==> invsqrt(b)
    // Square root has already been mirrored, but should be removed if unused
    case FltRecip(F( Op(FltSqrt(b)) )) => fltRecipSqrt(b).asInstanceOf[Sym[A]]
    case FixRecip(F( Op(FixSqrt(b)) )) => fixRecipSqrt(b).asInstanceOf[Sym[A]]

    // Multiple shitfies
    case FixSLA(F(Op(FixSLA(x: Fix[s,i,f],Const(y1)))), F(Const(y2))) => 
      fixShiftCombine(x, (y1+y2).toInt, "sla").asInstanceOf[Sym[A]]
    case FixSLA(F(Op(FixSRA(x: Fix[s,i,f],Const(y1)))), F(Const(y2))) =>   // For (x >> sr) << sl, ensure that the bits killed by >> stay killed
      super.transform(lhs,rhs)
    case FixSRA(F(Op(FixSRA(x: Fix[s,i,f],Const(y1)))), F(Const(y2))) => 
      fixShiftCombine(x, (y1+y2).toInt, "sra").asInstanceOf[Sym[A]]
    case FixSRA(F(Op(FixSLA(x: Fix[s,i,f],Const(y1)))), F(Const(y2))) => 
      super.transform(lhs,rhs)

    // m1*m2 + add --> fma(m1,m2,add)
    case FixAdd((mul@Op(FixMul(m1,m2))), F(add: Fix[s,i,f])) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      val specialAccum = if (lhs.getReduceCycle.isDefined) {lhs.reduceCycle.marker match {
        case AccumMarker.Reg.Op(_,_,_,_,_,_,_) => true
        case AccumMarker.Reg.FMA(_,_,_,_,_,_,_) => true
        case _ => false
      }} else false
      if (lhs.inCycle == mul.inCycle || specialAccum)
        transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }  // TODO: Set residual
      else 
        super.transform(lhs,rhs)

    case FixAdd(F(add: Fix[s,i,f]), F(mul@Op(FixMul(m1,m2)))) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      val specialAccum = if (lhs.getReduceCycle.isDefined) {lhs.reduceCycle.marker match {
        case AccumMarker.Reg.Op(_,_,_,_,_,_,_) => true
        case AccumMarker.Reg.FMA(_,_,_,_,_,_,_) => true
        case _ => false
      }} else false
      if (lhs.inCycle == mul.inCycle || specialAccum)
        transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }  // TODO: Set residual
      else 
        super.transform(lhs,rhs)

    case FltAdd(F(mul@Op(FltMul(m1,m2))), F(add: Flt[m,e])) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      val specialAccum = if (lhs.getReduceCycle.isDefined) {lhs.reduceCycle.marker match {
        case AccumMarker.Reg.Op(_,_,_,_,_,_,_) => true
        case AccumMarker.Reg.FMA(_,_,_,_,_,_,_) => true
        case _ => false
      }} else false
      if (lhs.inCycle == mul.inCycle || specialAccum)
        transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }
      else 
        super.transform(lhs,rhs)

    case FltAdd(F(add: Flt[m,e]), F(mul@Op(FltMul(m1,m2)))) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      val specialAccum = if (lhs.getReduceCycle.isDefined) {lhs.reduceCycle.marker match {
        case AccumMarker.Reg.Op(_,_,_,_,_,_,_) => true
        case AccumMarker.Reg.FMA(_,_,_,_,_,_,_) => true
        case _ => false
      }} else false
      if (lhs.inCycle == mul.inCycle || specialAccum)
        transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }
      else 
        super.transform(lhs,rhs)

    // Not rewrite, but set residual metadata on certain patterns
    case Op(FixAdd(F(xx), Final(ofs))) if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, ofs, 0)
      m
        
    case Op(FixAdd(Final(ofs), F(xx)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, ofs, 0)
      m

        
    case Op(FixSub(F(xx), Final(ofs)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, -ofs, 0)
      m

    case Op(FixMul(Final(lin), F(xx)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, 0, 0)
      m

    case Op(FixMul(F(xx), Final(lin)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, 0, 0)
      m

    case Op(FixFMA(Final(lin), F(xx), Final(ofs)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, ofs, 0)
      m

    case Op(FixFMA(F(xx), Final(lin), Final(ofs)))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, ofs, 0)
      m

    case _ => super.transform(lhs,rhs)
  }

}
