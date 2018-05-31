package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.internal._
import spatial.util.shouldMotion
import spatial.traversal.AccelTraversal
import spatial.data._
import spatial.util._

/** Converts inner pipes that contain switches into innerpipes with enabled accesses.
  * Also squashes outer unit pipes that contain only one child
  */
case class FlatteningTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  private var enables: Set[Bit] = Set.empty

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ super.transform(lhs,rhs) }

    case UnitPipe(en, body) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val body2 = getPrimitives(rhs, body)
      val lhs2 = stageWithFlow(UnitPipe(en, body2)){lhs2 => transferData(lhs,lhs2) }
      lhs2


    case UnrolledForeach(ens,cchain,body,iters,valids) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val body2 = getPrimitives(rhs, body)
      val lhs2 = stageWithFlow(UnrolledForeach(ens, cchain, body2, iters, valids)){lhs2 => transferData(lhs,lhs2) }
      lhs2

    case UnrolledReduce(ens,cchain,body,iters,valids) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val body2 = getPrimitives(rhs, body)
      val lhs2 = stageWithFlow(UnrolledReduce(ens, cchain, body2, iters, valids)){lhs2 => transferData(lhs,lhs2) }
      lhs2

    case _ => super.transform(lhs,rhs)
  }

  private def getPrimitives[A:Type](rhs: Op[A], body: Block[_]): Block[Void] = {
      val regular_primitives = rhs.blocks.head.stms.collect{case Primitive(s) => s; case s @ Op(o: Switch[_]) if (!o.R.isBits) => s}
      val switches = rhs.blocks.head.stms.collect{case s @ Op(o: Switch[_]) if (o.R.isBits) => (s,o)}
      val flattened_primitives = switches.map{case(sym,op) => squash(sym)}.flatten
      // val ohmuxes = switches.collect{case(sym,op) if (op.R.isBits) => 
      //   val Op(Switch(selects, _)) = sym
      //   val mux = stage(OneHotMux(selects, op.cases.map{x => f(x.body.result.asInstanceOf[Bits[A]])}))
      //   register(sym -> mux)
      //   mux
      // }
      dbgs(s"New body for $rhs will contain:")
      regular_primitives.foreach{x => dbgs(s" $x - ${x.op}")}
      flattened_primitives.foreach{x => dbgs(s" * $x - ${x.op} (from switch)")}
      val body2 = stageScope(f(body.inputs),body.options){
        (regular_primitives ++ flattened_primitives/* ++ ohmuxes*/).foreach(visit)
        assert(body.result.isVoid)
        void
      }
      body2

  }

  private def squash(s: Sym[_]): List[Sym[_]] = {
    s match {
      case Op(Switch(selects, body)) => 
        selects.zip(s.children.map(_.s.get)).map{case (sel, sc) => 
          if (sc.children.length >= 1) sc.children.map{x => squash(x.s.get)}.flatten
          else sc.blocks.head.stms.toList
        }.toList.flatten
      case _ => throw new Exception()
    }
  }

}


