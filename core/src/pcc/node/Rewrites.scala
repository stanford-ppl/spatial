package pcc.node

import forge._
import pcc.core._
import pcc.lang._

trait Rewrites {
  rewrite.add[FixDiv[_]]{case FixDiv(a,Const(1)) => a }
}
