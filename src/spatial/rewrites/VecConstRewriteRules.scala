package spatial.rewrites

import argon._
import spatial.node._
import spatial.lang._
import argon.node._
import emul.FixedPoint
import spatial.metadata.bounds._

trait VecConstRewriteRules extends RewriteRules {
  private implicit val state:State = IR 

  IR.rewrites.addGlobal("VecConstProp", {
    case (op@FixSRA(a, b), ctx, _) => 
      VecConst.broadcast(a,b) { 
        case (x, y) if y >= 0 => x >> y
        case (x, y) if y < 0 => x << -y
      }(op.R,state)
    case (op:Binary[c,r], ctx, _) => 
      (op.a, op.b) match {
        case (Const(a), Const(b)) => Some(op.R.from(op.unstaged(a, b)))
        case (a,b) =>
          VecConst.broadcast(a, b) { (a,b) => op.unstaged(a.asInstanceOf[c], b.asInstanceOf[c]) }(op.R,state)
      }
    case _ => None
  })
}
