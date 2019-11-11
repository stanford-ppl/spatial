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
import forge.tags._
import emul.FixedPoint

import utils.math.{isPow2,log2,gcd}
import spatial.util.math._
import utils.math.{isSumOfPow2,asSumOfPow2,isMersenne,withinNOfMersenne}

/** Performs hardware-specific rewrite rules. */
case class RewriteTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  /** Check whether it is a good idea to fuse.  It is a good idea if either:
    *  1) The mul and the add are both in a reduction cycle, OR neither are in a reduction cycle
    *  2) The add is marked as a to-be-specialized node, i.e. will become part of RegAccumFMA
    *  3) forceFuseFMA flag was set
    */
  private def specializationFuse(add: Sym[_], mul: Sym[_]): Boolean = {
    val specialAccum = if (add.getReduceCycle.isDefined) {add.reduceCycle.marker match {
      case AccumMarker.Reg.Op(_,_,_,_,_,_,_) => true
      case AccumMarker.Reg.FMA(_,_,_,_,_,_,_) => true
      case _ => false
    }} else false
    add.inCycle == mul.inCycle || specialAccum || spatialConfig.forceFuseFMA
  }

  def rewriteMul[S,I,F](a: Fix[S,I,F], q: Int, mul1: Int, mul2: Int, dir: String): Fix[S,I,F] = {
    implicit val S: BOOL[S] = a.fmt.s
    implicit val I: INT[I] = a.fmt.i
    implicit val F: INT[F] = a.fmt.f
    dir match {
      case "add" if q > 0 => stage(FixAdd(stage(FixMul(a, Type[Fix[S,I,F]].from(mul1))), stage(FixMul(a, Type[Fix[S,I,F]].from(mul2)))))
      case "sub" if q > 0 => stage(FixSub(stage(FixMul(a, Type[Fix[S,I,F]].from(mul1))), stage(FixMul(a, Type[Fix[S,I,F]].from(mul2)))))
      case "add" if q < 0 => stage(FixAdd(stage(FixMul(a, Type[Fix[S,I,F]].from(-mul1))), stage(FixMul(a, Type[Fix[S,I,F]].from(-mul2)))))
      case "sub" if q < 0 => stage(FixSub(stage(FixMul(a, Type[Fix[S,I,F]].from(-mul1))), stage(FixMul(a, Type[Fix[S,I,F]].from(-mul2)))))
      case _ => throw new Exception(s"Something very bad happened while trying to rewrite $a * $q!  ($mul1 $mul2 $dir)")
    }
  }

  // Magical rewrite rules, based loosely on http://homepage.divms.uiowa.edu/~jones/bcd/mod.shtml#exmod7
  def rewriteModWithMersenne[S,I,F](a: Fix[S,I,F], mod: Int): Fix[S,I,F] = {
    implicit val S: BOOL[S] = a.fmt.s
    implicit val I: INT[I] = a.fmt.i
    implicit val F: INT[F] = a.fmt.f
    val pow = (scala.math.log(mod+1) / scala.math.log(2)).toInt
    val levels = ({if (mod == 3) List(pow,pow) else List(pow)} ++ List.tabulate(31){i => i+1}.collect{case i if (i % pow == 0) && isPow2(i / pow) => i}).reverse
    dbgs(s"Rewriting $a mod $mod with $levels bitshifts")
    val temps: scala.collection.mutable.ListBuffer[Fix[S,I,F]] = scala.collection.mutable.ListBuffer.fill(levels.size)(a)
    temps(0) = stage(FixAdd[S,I,F](stage(FixSRA(a,levels.head)), stage(FixAnd(a,Type[Fix[S,I,F]].from(scala.math.pow(2,levels.head).toInt-1)))))
    List.tabulate(levels.size-1) { level =>
      temps(level+1) = stage(FixAdd[S,I,F](
                                stage(FixSRA(temps(level),levels(level+1))),
                                stage(FixAnd(temps(level),Type[Fix[S,I,F]].from(scala.math.pow(2,levels(level+1)).toInt-1))))
                          )
    }
    val boundaries = if (mod == 3) Seq(3,6) else Seq.tabulate(3){i => mod*(i+1)}
    stage(PriorityMux(boundaries.map{b => temps.last < b}, boundaries.map{b => temps.last - (b - mod)}))
  }

  def rewriteMod[S,I,F](a: Fix[S,I,F], mod: Int, closestMersenne: Int): Fix[S,I,F] = {
    implicit val S: BOOL[S] = a.fmt.s
    implicit val I: INT[I] = a.fmt.i
    implicit val F: INT[F] = a.fmt.f
    val bigmod = rewriteModWithMersenne(a, closestMersenne)
    dbgs(s"Rewriting $a mod $mod as a function of $closestMersenne")
    val boundaries = Seq.tabulate(closestMersenne / mod){i => mod*(i+1)}
    stage(PriorityMux(boundaries.map{b => bigmod < Type[Fix[S,I,F]].from(b)}, boundaries.map{b => bigmod - Type[Fix[S,I,F]].from(b - mod)}))
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

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }

    case FixMul(a: Fix[s,i,f], Const(q)) if inHw && spatialConfig.optimizeMul && (q.toDouble % 1.0 == 0.0) && isSumOfPow2(scala.math.abs(q.toInt)) =>
      val (mul1, mul2, dir) = asSumOfPow2(scala.math.abs(q.toInt))
      transferDataToAllNew(lhs){ rewriteMul(a,q.toInt,mul1,mul2,dir).asInstanceOf[A] }

    case FixMod(a: Fix[s, i, f], Const(q)) if inHw && spatialConfig.optimizeMod && (q.toDouble % 1.0 == 0.0) && isMersenne(q.toInt) =>
      transferDataToAllNew(lhs){ rewriteModWithMersenne(a, q.toInt).asInstanceOf[A] }

    case FixMod(a: Fix[s, i, f], Const(q)) if inHw && spatialConfig.optimizeMod && (q.toDouble % 1.0 == 0.0) && withinNOfMersenne(16,q.toInt).isDefined =>
      transferDataToAllNew(lhs){ rewriteMod(a, q.toInt, withinNOfMersenne(16,q.toInt).get).asInstanceOf[A] }


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
      val (iter, add, mul) = x match {
        case Op(FixAdd(F(xx), Final(ofs))) => (xx, ofs, 1)
        case Op(FixAdd(Final(ofs), F(xx))) => (xx, ofs, 1)
        case Op(FixSub(F(xx), Final(ofs))) => (xx, -ofs, 1)
        case Op(FixMul(Final(lin), F(xx))) => (xx, 0, lin)
        case Op(FixMul(F(xx), Final(lin))) => (xx, 0, lin)
        case Op(FixFMA(Final(lin), F(xx), Final(ofs))) => (xx, ofs, lin)
        case Op(FixFMA(F(xx), Final(lin), Final(ofs))) => (xx, ofs, lin)
        case _ => (x, 0, 1)
      }

      if (iter.getCounter.isDefined && staticMod(mul, iter, y)) {
        // Convert FixMod node into a Const or Const vec
        val posMod = getPosMod(mul, iter, add, y)
        transferDataToAllNew(lhs){ constMod(iter, posMod).asInstanceOf[Sym[A]] }
      } else if (iter.getCounter.isDefined && isPow2(y)) {
        val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
        dbgs(s"Cannot statically determine $x % $y")
        m.modulus = y
        m.residual = residual(1, iter, add, y)
        m     
      } else if (isPow2(y)) {
        val m = transferDataToAllNew(lhs){ selectMod(x, y).asInstanceOf[Sym[A]] }
        dbgs(s"Cannot statically determine $x % $y")
        m.modulus = y
        m.residual = ResidualGenerator(1,0,y)
        m     
      } else {
        val m = super.transform(lhs,rhs)
        m.residual = residual(1, iter, add, y)
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
    case FixAdd(F(mul@Op(FixMul(m1,m2))), F(add: Fix[s,i,f])) if lhs.canFuseAsFMA && specializationFuse(lhs, mul) =>
      transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }  // TODO: Set residual

    case FixAdd(F(add: Fix[s,i,f]), F(mul@Op(FixMul(m1,m2)))) if lhs.canFuseAsFMA && specializationFuse(lhs, mul) =>
      transferDataToAllNew(lhs){ fixFMA(m1,m2,add).asInstanceOf[Sym[A]] }  // TODO: Set residual

    case FltAdd(F(mul@Op(FltMul(m1,m2))), F(add: Flt[m,e])) if lhs.canFuseAsFMA && specializationFuse(lhs, mul) =>
      transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    case FltAdd(F(add: Flt[m,e]), F(mul@Op(FltMul(m1,m2)))) if lhs.canFuseAsFMA && specializationFuse(lhs, mul) =>
      transferDataToAllNew(lhs){ fltFMA(m1,m2,add).asInstanceOf[Sym[A]] }

    // Not rewrite, but set residual metadata on certain patterns
    case FixAdd(F(xx), Final(ofs)) if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, ofs, 0)
      m
        
    case FixAdd(Final(ofs), F(xx))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, ofs, 0)
      m

        
    case FixSub(F(xx), Final(ofs))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(1, xx, -ofs, 0)
      m

    case FixMul(Final(lin), F(xx))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, 0, 0)
      m

    case FixMul(F(xx), Final(lin))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, 0, 0)
      m

    case FixFMA(Final(lin), F(xx), Final(ofs))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, ofs, 0)
      m

    case FixFMA(F(xx), Final(lin), Final(ofs))  if inHw => 
      val m = super.transform(lhs,rhs)
      m.residual = residual(lin, xx, ofs, 0)
      m
      
    case _ => super.transform(lhs,rhs)
  }

}
