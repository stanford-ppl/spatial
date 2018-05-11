package spatial.rewrites

import argon._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util._

import utils.implicits.collections._

trait MuxRewrites extends RewriteRules {

  @rewrite def rewrite_mux(op: Mux[_]): Sym[_] = {
    case Mux(Literal(true), a, _)  => a
    case Mux(Literal(false), _, b) => b
    case Mux(_, a, b) if a == b    => a
  }

  @rewrite def rewrite_oneHotMux(op: OneHotMux[_]): Sym[_] = {
    case OneHotMux(sels, vals) if sels.length == 1 => vals.head
    case OneHotMux(sels, vals) if sels.exists{case Literal(true) => true; case _ => false} =>
      val trues = sels.zipWithIndex.filter{case (Literal(true),_) => true; case _ => false }
      if (trues.lengthMoreThan(1)) {
        warn(ctx, "One-hot mux has multiple statically true selects")
        warn(ctx)
      }
      val idx = trues.head._2
      vals(idx)

    case OneHotMux(sels, vals) if vals.distinct.lengthIs(1) =>
      vals.head
  }


}
