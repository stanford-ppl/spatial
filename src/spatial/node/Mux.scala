package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import utils.implicits.collections._

@op case class Mux[A:Bits](s: Bit, a: Bits[A], b: Bits[A]) extends Primitive[A] {

  @rig override def rewrite: A = s match {
    case Literal(true)  => a.unbox
    case Literal(false) => b.unbox
    case _ if a == b    => a.unbox
    case _ => super.rewrite
  }

}
@op case class OneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[Bits[A]]) extends Primitive[A] {

  @rig override def rewrite: A = {
    if (sels.length == 1) vals.head.unbox
    else if (sels.exists{case Literal(true) => true; case _ => false}) {
      val trues = sels.zipWithIndex.filter{case (Literal(true), _) => true; case _ => false }
      if (trues.lengthMoreThan(1)) {
        warn(ctx, "One-hot mux has multiple statically true selects")
        warn(ctx)
        throw new Exception(s"Behavior when multiple statically true selects is undefined:")
      }
      val idx = trues.head._2
      vals(idx).unbox
    }
    else if (vals.distinct.lengthIs(1)) {
      vals.head.unbox
    }
    else super.rewrite
  }
}

@op case class PriorityMux[A:Bits](sels: Seq[Bit], vals: Seq[Bits[A]]) extends Primitive[A] {

  @rig override def rewrite: A = {
    if (sels.length == 1) vals.head.unbox
//    else if (sels.exists{case Literal(true) => true; case _ => false}) {
//      val trues = sels.zipWithIndex.filter{case (Literal(true), _) => true; case _ => false }
//      if (trues.lengthMoreThan(1)) {
//        warn(ctx, "Priority mux has multiple statically true selects")
//        warn(ctx)
//      }
//      val idx = trues.head._2
//      vals(idx).unbox
//    }
    else if (vals.distinct.lengthIs(1)) {
      vals.head.unbox
    }
    else super.rewrite
  }
}
