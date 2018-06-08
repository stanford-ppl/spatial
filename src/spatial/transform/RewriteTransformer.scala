package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.internal.spatialConfig

import utils.math.{isPow2,log2}

case class RewriteTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  def selectMod[S:BOOL,I:INT,F:INT](x: FixPt[S,I,F], y: Int): FixPt[S,I,F] = {
    val data = x.asBits
    val range = (log2(y.toDouble)-1).toInt :: 0
    val selected = data.apply(range)
    selected.as[Fix[S,I,F]]
  }

  def writeReg[A](lhs: Sym[_], reg: Reg[_], data: Bits[A], ens: Set[Bit]): Void = {
    implicit val A: Bits[A] = data.selfType
    stageWithFlow( RegWrite[A](reg.asInstanceOf[Reg[A]], data, ens) ){lhs2 => transferData(lhs, lhs2) }
  }

  def fltFMA[M,E](m1: Flt[M,E], m2: Flt[M,E], add: Flt[M,E]): Flt[M,E] = {
    import add.fmt._
    stage(FltFMA(m1,m2,add))
  }

  def fltRecipSqrt[M,E](x: Flt[M,E]): Flt[M,E] = {
    import x.fmt._
    stage(FltRecipSqrt(x))
  }



  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }

    // Change a write from a mux with the register or some other value to an enabled register write
    case RegWrite(F(reg), F(data), F(en)) => data match {
      case Op( Mux(sel, Op(e@RegRead(`reg`)), b) ) =>
        val lhs2 = writeReg(lhs, reg, b, en + !sel)
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      case Op( Mux(sel, a, Op(e @ RegRead(`reg`))) ) =>
        val lhs2 = writeReg(lhs, reg, a, en + sel)
        dbg(s"Rewrote ${stm(lhs)}")
        dbg(s"  to ${stm(lhs2)}")
        lhs2.asInstanceOf[Sym[A]]

      case _ => super.transform(lhs, rhs)
    }

    case op @ FixMod(F(x), F(Final(y))) if isPow2(y) =>
      import x.fmt._
      selectMod(x, y).asInstanceOf[Sym[A]]

    // 1 / sqrt(b)  ==> invsqrt(b)
    // Square root has already been mirrored, but should be removed if unused
    // TODO[5]: Why do the Flt nodes require separate methods to type check properly??
    case FltRecip(F( Op(FltSqrt(b)) )) => fltRecipSqrt(b).asInstanceOf[Sym[A]]

    case FixRecip(F( Op(FixSqrt(b: Fix[s,i,f])) )) =>
      import b.fmt._
      stage(FixRecipSqrt[s,i,f](b)).asInstanceOf[Sym[A]]


    // m1*m2 + add --> fma(m1,m2,add)
    // TODO[1]: Consumers check is wrong here now that this is a mutate transformer!
    case FixAdd(mul@F(Op(FixMul(m1,m2))), F(add: Fix[s,i,f])) if mul.consumers.size == 1 =>
      import add.fmt._
      stage(FixFMA[s,i,f](m1,m2,add)).asInstanceOf[Sym[A]]

    case FixAdd(F(add: Fix[s,i,f]), mul@F(Op(FixMul(m1,m2)))) if mul.consumers.size == 1 =>
      import add.fmt._
      stage(FixFMA[s,i,f](m1,m2,add)).asInstanceOf[Sym[A]]

    case FltAdd(mul@F(Op(FltMul(m1,m2))), F(add: Flt[m,e])) if mul.consumers.size == 1 =>
      fltFMA(m1,m2,add).asInstanceOf[Sym[A]]

    case FltAdd(F(add: Flt[m,e]), mul@F(Op(FltMul(m1,m2)))) if mul.consumers.size == 1 =>
      fltFMA(m1,m2,add).asInstanceOf[Sym[A]]

    case _ => super.transform(lhs,rhs)
  }

}
