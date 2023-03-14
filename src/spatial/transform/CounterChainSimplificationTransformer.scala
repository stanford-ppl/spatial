package spatial.transform

import argon.transform.ForwardTransformer
import spatial.node._
import argon._
import spatial.lang.Counter
import spatial.metadata.control._

case class CounterChainSimplificationTransformer(IR: argon.State) extends ForwardTransformer {
  var remapped: Set[Counter[_]] = Set.empty
  var removedCChains: Set[Sym[_]] = Set.empty
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (lhs match {
    case ctr@Op(ctrn@CounterNew(start, end, step, par@Const(c)))
      if c.toInt == 1 && ctr.asInstanceOf[Counter[_]].nIters.exists(_.toInt == 1) =>
      dbgs(s"Eliding counter: ${stm(lhs)} [$start, $end, $step, $par]")
      // In this case, the ctr is a single iteration mapped to the start
      val casted: Counter[_] = ctr.asInstanceOf[Counter[_]]
      register(casted.iter.get -> f(start))
      remapped += casted
      Invalid

    case cchain@Op(counterChainNew@CounterChainNew(ctrs)) =>
      val unmappedCtrs = ctrs.filterNot(remapped.contains)
      if (unmappedCtrs.nonEmpty) {
        stageWithFlow(CounterChainNew(f(unmappedCtrs))) {
          newCChain =>
            transferData(lhs, newCChain)
        }
      } else {
        dbgs(s"Eliding counterchain ${cchain}[${ctrs.mkString}] entirely!")
        removedCChains += lhs
        Invalid // do nothing
      }

    case Op(OpForeach(ens, cchain, block, iters, stopWhen)) if removedCChains.contains(cchain) =>
      dbgs(s"Entire cchain was elided, replacing with a unitpipe")
      stageWithFlow(UnitPipe(f(ens), stageBlock {
        inlineBlock(block)
      }, f(stopWhen))) {
        newCtrl => transferData(lhs, newCtrl)
      }

    case Op(OpForeach(ens, cchain, block, iters, stopWhen)) if cchain.counters.toSet.intersect(remapped).nonEmpty =>
      val newIters = iters.filterNot{ iter => remapped contains iter.counter.ctr}
      stageWithFlow(OpForeach(ens, f(cchain), stageBlock {inlineBlock(block)}, newIters, f(stopWhen))) {
        lhs2 => transferData(lhs, lhs2)
      }

//    case Op(OpReduce(ens, cchain, block, iters, stopWhen)) if removedCChains.contains(cchain) =>
//      // This just becomes a degenerate pipeline

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

  override def postprocess[R](block: Block[R]): Block[R] = {
    remapped = Set.empty
    super.postprocess(block)
  }
}
