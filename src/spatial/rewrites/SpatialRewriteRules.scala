package spatial.rewrites

import argon._

case class SpatialRewriteRules(IR: State) extends RewriteRules
  with AliasRewrites with LUTConstReadRewriteRules with VecConstRewriteRules
  with CounterIterRewriteRule

