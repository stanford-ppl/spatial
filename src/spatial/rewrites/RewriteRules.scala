package spatial.rewrites

import forge.tags._
import core._
import emul.{FixedPoint, FloatPoint}
import spatial.node._
import spatial.lang._

// FIXME: It's a little too easy to write something horribly broken here
trait RewriteRules {
  type Fx = FixedPoint
  type Ft = FloatPoint

  @rewrite def fixneg(op: FixNeg[_,_,_]): Sym[_] = {
    case FixNeg(Const(a:Fx)) => op.tR.from(-a)
    case FixNeg(Op(FixNeg(a:Fix[_,_,_]))) => a
  }

  @rewrite def fixadd(op: FixAdd[_,_,_]): Sym[_] = {
    case FixAdd(Const(a:Fx),Const(b:Fx)) => op.tR.from(a + b)
    case FixAdd(a:Fix[_,_,_],Const(0)) => a
    case FixAdd(Const(0),a:Fix[_,_,_]) => a
  }
  @rewrite def fixsub(op: FixSub[_,_,_]): Sym[_] = {
    case FixSub(Const(a:Fx),Const(b:Fx)) => op.tR.from(a - b)
    case FixSub(a:Fix[_,_,_],Const(0)) => a
    //case FixSub(Const(0),a:Fix[_]) => (-a).viewSym(op.tR)
  }

  @rewrite def fixmul(op: FixMul[_,_,_]): Sym[_] = {
    case FixMul(Const(a:Fx),Const(b:Fx)) => op.tR.from(a * b)
    //case FixMul(_,z@Const(0)) => z
    //case FixMul(z@Const(0),_) => z
    case FixMul(a:Fix[_,_,_],Const(1)) => a
    case FixMul(Const(1),b:Fix[_,_,_]) => b
  }

  @rewrite def fixdiv(op: FixDiv[_,_,_]): Sym[_] = {
    case FixDiv(_, Const(c:Fx)) if c === 0 =>
      warn(ctx, s"Constant ${op.tR} division by 0")
      warn(ctx)
      Invalid // Ignored; not a subtype of the expected type

    case FixDiv(Const(a:Fx),Const(b:Fx)) => op.tR.from(a / b)
    case FixDiv(a:Fix[_,_,_],Const(1)) => a
   // case FixDiv(z@Const(0),_) => z
  }
}
