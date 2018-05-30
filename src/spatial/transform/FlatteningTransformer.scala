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

    case UnitPipe(body, en) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val regular_primitives = rhs.blocks.head.stms.collect{case Primitive(s) => s}
      val switches = rhs.blocks.head.stms.collect{case s @ Op(_: Switch[_]) => s}
      val flattened_primitives = switches.map{x => 
        // if (x.R.isBits) {
        //   dbgs(s" - Squashing $x (returns bits)")
        // }
        // else dbgs(s" - Squashing $x")
        squash(x)
      }.flatten
      val new_primitives = regular_primitives ++ flattened_primitives
      dbgs(s"New body for $lhs will contain:")
      regular_primitives.foreach{x => dbgs(s" $x - ${x.op}")}
      flattened_primitives.foreach{x => dbgs(s" * $x - ${x.op} (from switch)")}
      // val lhs2 = stageWithFlow(UnitPipe(body,en)){lhs2 => transferData(lhs,lhs2) }

      // lhs2
      super.transform(lhs,rhs)

    case UnrolledForeach(ens,cchain,body,iters,valids) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val regular_primitives = rhs.blocks.head.stms.collect{case Primitive(s) => s}
      val switches = rhs.blocks.head.stms.collect{case s @ Op(_: Switch[_]) => s}
      val flattened_primitives = switches.map{x => 
        // if (x.R.isBits) {
        //   dbgs(s" - Squashing $x (returns bits)")
        // }
        // else dbgs(s" - Squashing $x")
        squash(x)
      }.flatten
      val new_primitives = regular_primitives ++ flattened_primitives
      dbgs(s"New body for $lhs will contain:")
      regular_primitives.foreach{x => dbgs(s" $x - ${x.op}")}
      flattened_primitives.foreach{x => dbgs(s" * $x - ${x.op} (from switch)")}
      // val lhs2 = stageWithFlow(UnrolledForeach(ens, cchain, body, iters, valids)){lhs2 => transferData(lhs,lhs2) }

      // lhs2
      super.transform(lhs,rhs)

    case UnrolledReduce(ens,cchain,body,iters,valids) if (inHw && lhs.isInnerControl && lhs.children.length >= 1) =>
      dbgs(s"Transforming $lhs")
      val regular_primitives = rhs.blocks.head.stms.collect{case Primitive(s) => s}
      val switches = rhs.blocks.head.stms.collect{case s @ Op(_: Switch[_]) => s}
      val flattened_primitives = switches.map{x => 
        // if (x.R.isBits) {
        //   dbgs(s" - Squashing $x (returns bits)")
        // }
        // else dbgs(s" - Squashing $x")
        squash(x)
      }.flatten
      val new_primitives = regular_primitives ++ flattened_primitives
      dbgs(s"New body for $lhs will contain:")
      regular_primitives.foreach{x => dbgs(s" $x - ${x.op}")}
      flattened_primitives.foreach{x => dbgs(s" * $x - ${x.op} (from switch)")}
      // val lhs2 = stageWithFlow(UnrolledReduce(ens, cchain, body, iters, valids)){lhs2 => transferData(lhs,lhs2) }

      // lhs2
      super.transform(lhs,rhs)

    case _ => super.transform(lhs,rhs)
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


