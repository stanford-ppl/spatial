package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.metadata.rewrites._
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

  def constMod[S,I,F](x: FixPt[S,I,F], y: Int): FixPt[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    x.from(y)
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

  def static(iter: Num[_], y: scala.Int): Boolean = {
    if (iter.counter.ctr.isStaticStartAndStep) {
      val Final(step) = iter.counter.ctr.step
      val par = iter.counter.ctr.ctrPar.toInt
      val lane = iter.counter.lane
      (par*step % y) == 0     
    } else false
  }

  def getPosMod(x: Num[_], ofs: Int, mod: Int): Int = {
    val Final(start) = x.counter.ctr.start
    val Final(step) = x.counter.ctr.step
    val par = x.counter.ctr.ctrPar.toInt
    val lane = x.counter.lane
    val r = start + lane * step + ofs
    val posMod = ((r % mod) + mod) % mod
    dbgs(s"Replace $x + $ofs % $mod with ${posMod} (ctr start $start, step $step, lane $lane)")
    posMod
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
        println(s"b is $b ${b.tp}")
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      // Change a write from a mux with the register or some other value to an enabled register write
      case Op( Mux(sel, a, Op(RegRead(`reg`))) ) =>
        val lhs2 = writeReg(lhs, reg, a, en + sel)
        println(s"a is $a ${a.tp}")
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      case _ => super.transform(lhs, rhs)
    }

    case FixMod(F(x), F(Final(y))) if isPow2(y) && inHw => 
      x match {
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixAdd(F(xx), Final(yy))) if (xx.getCounter.isDefined && static(xx, y)) => 
          val posMod = getPosMod(xx, yy, y)
          transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixAdd(Final(yy), F(xx))) if (xx.getCounter.isDefined && static(xx, y)) => 
          val posMod = getPosMod(xx, yy, y)
          transferDataToAllNew(lhs){ constMod(xx, posMod).asInstanceOf[Sym[A]] }
        // Assume chained arithmetic will have been constant propped by now
        case Op(FixSub(F(xx), Final(yy))) if (xx.getCounter.isDefined && static(xx, y)) => 
          val posMod = getPosMod(xx, -yy, y)
          transferDataToAllNew(lhs){ constMod(x, posMod).asInstanceOf[Sym[A]] }
        case _ if (x.getCounter.isDefined && static(x, y)) =>
          val posMod = getPosMod(x, 0, y)
          transferDataToAllNew(lhs){ constMod(x, posMod).asInstanceOf[Sym[A]] }
        case _ => 
          val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
          dbgs(s"Cannot statically determine $x % $y")
          m.modulus = y
          m        
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
    case FixAdd((Op(FixMul(m1,m2))), F(add: Fix[s,i,f])) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    case FixAdd(F(add: Fix[s,i,f]), F(Op(FixMul(m1,m2)))) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    case FltAdd(F(Op(FltMul(m1,m2))), F(add: Flt[m,e])) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    case FltAdd(F(add: Flt[m,e]), F(Op(FltMul(m1,m2)))) if lhs.canFuseAsFMA && spatialConfig.fuseAsFMA =>
      transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    case _ => super.transform(lhs,rhs)
  }

}
