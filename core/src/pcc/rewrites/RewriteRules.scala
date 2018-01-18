package pcc.rewrites

import forge._
import pcc.core._
import pcc.node.FixDiv

trait RewriteRules {
  //rewrite.add[FixDiv[_]]{case FixDiv(a,Const(1)) => a }

  @rewrite def fixdiv(x: FixDiv[_]) = {case FixDiv(a,Const(1)) => a }
}
