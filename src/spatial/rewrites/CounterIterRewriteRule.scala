package spatial.rewrites

import argon._
import spatial.node._
import spatial.lang._
import argon.node._
import emul.FixedPoint
import spatial.metadata.bounds._
import spatial.metadata.control._
import forge.tags._

trait CounterIterRewriteRule extends RewriteRules {
  private implicit val state:State = IR 

  /*
   * (iter - start) / step % par is the post unrolling lane id
   * */
  IR.rewrites.add[FixMod[_,_,_]]("UnrollLane", {
    // iter % par if start == 0 and step == 1
    case (op@FixMod(iter@PreunrollIter(Final(0),end,Final(1),Final(par)),Final(par2)), ctx, _) if par == par2 =>
      Some(stage(LaneStatic(iter.asInstanceOf[FixPt[TRUE,_32,_0]], List.tabulate(par) { i => i })))
    // (iter / step) % par if start = 0
    case (op@FixMod(
      Def(FixDiv(iter@PreunrollIter(Final(0),end,step,Final(par)), step2)),
      Final(par2)
    ), ctx, _) if par == par2 & step2 == step =>
      Some(stage(LaneStatic(iter.asInstanceOf[FixPt[TRUE,_32,_0]], List.tabulate(par) { i => i })))
    // (iter - start / step) % par
    case (op@FixMod(
      Def(FixDiv(
        Def(FixSub(iter@PreunrollIter(start,end,step,Final(par)), start2)), 
        step2
      )),
      Final(par2)
    ), ctx, _) if par == par2 & step2 == step & start2 == start =>
      Some(stage(LaneStatic(iter.asInstanceOf[FixPt[TRUE,_32,_0]], List.tabulate(par) { i => i })))
    case _ => None
  })

}

object PreunrollIter {
  @stateful def unapply(sym:Num[_]) = sym match {
    case sym if sym.isBound => sym.getCounter.flatMap { 
      case IndexCounterInfo(Def(CounterNew(start,end,step,spar@Expect(par))), lanes) =>
        if (lanes.size == par) Some((start,end,step,spar)) else None
      case _ => None
    }
    case _ => None
  }
}
