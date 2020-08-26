package spatial.dse

import argon._
import forge.tags.data
import forge.tags.stateful
import utils.recursive._
import spatial.metadata.params._
import spatial.metadata.control._
import spatial.metadata.types._

trait SpaceGenerator {
  final val PRUNE: Boolean = false

  implicit class ToRange(x: (Int,Int,Int)) {
    def toRange: Range = x._1 to x._3 by x._2
  }

  def domain(p: Sym[_], restricts: Iterable[Restrict])(implicit baseIR: State): Domain[Int] = {
    val prior = p.getPrior

    if (restricts.nonEmpty) {
      val range = p.paramDomain match {
        case Left(x) => Left(x.toRange)
        case Right(x) => Right(x)
      }
      Domain.restricted(
        name   = p.name.getOrElse(s"$p"),
        id     = p.hashCode,
        range  = range,
        setter = {(v: Int, state: State) => p.setIntValue(v)(state) },
        getter = {(state: State) => p.intValue(state).toInt },
        cond   = {state => restricts.forall(_.evaluate()(state)) },
        tp     = Ordinal(prior)
      )
    }
    else {
      val range = p.paramDomain match {
        case Left(x) => Left(x.toRange)
        case Right(x) => Right(x)
      }
      Domain(
        name  = p.name.getOrElse(s"$p"),
        id = p.hashCode,
        range = range,
        setter = { (v: Int, state: State) => p.setIntValue(v)(state) },
        getter = { (state: State) => p.intValue(state).toInt },
        tp     = Ordinal(prior)
      )
    }
  }

  def createIntSpace(params: Seq[Sym[_]], restrict: Set[Restrict])(implicit baseIR: State): Seq[Domain[Int]] = {
    if (PRUNE) {
      val pruneSingle = params.map { p =>
        val restricts = restrict.filter(_.dependsOnlyOn(p))
        p -> domain(p, restricts)
      }
      pruneSingle.map(_._2)
    }
    else {
      params.map{p => domain(p, Nil) }
    }
  }

  def createCtrlSpace(metapipes: Seq[Sym[_]])(implicit baseIR: State): Seq[Domain[Boolean]] = {
    metapipes.map{mp =>
      new Domain[Boolean](
        name    = mp.name.getOrElse(s"$mp"),
        id      = mp.hashCode,
        options = Seq(false, true),
        setter  = {(c: Boolean, state:State) => if (c) mp.setSchedValue(Pipelined)(state)
                                                else   mp.setSchedValue(Sequenced)(state) },
        getter  = {(state: State) => mp.schedValue(state) == Pipelined },
        tp      = Categorical(Seq(0.5,0.5))
      )
    }
  }
}

