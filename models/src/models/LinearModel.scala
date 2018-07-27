package models

import scala.collection.mutable
import scala.util.Try

case class Prod(a: Double, xs: Seq[String]) {
  def unary_-(): Prod = Prod(-a,xs)
  def *(b: String): Prod = Prod(a, b +: xs)
  def *(b: Double): Prod = Prod(a*b,xs)
  def /(b: Double): Prod = Prod(a/b,xs)
  def eval(vals: Map[String,Double]): Double = a * xs.map{x => vals(x)}.product
  def partial(vals: Map[String,Double]): Prod = {
    val (res, un) = xs.partition(x => vals.contains(x))
    val newa = a * res.map{x => vals(x) }.product
    Prod(newa, un)
  }
  override def toString: String = if (xs.nonEmpty) s"$a * " + xs.mkString("*") else a.toString
}

case class LinearModel(exp: Seq[Prod], vars: Set[String]) {
  def unary_-(): LinearModel = LinearModel(exp.map(e => -e), vars)
  def *(b: String): LinearModel = LinearModel(exp.map(_*b),vars + b)
  def *(b: Double): LinearModel = LinearModel(exp.map(_*b),vars)
  def /(b: Double): LinearModel = LinearModel(exp.map(_/b),vars)
  def +(b: Double): LinearModel = {
    val (sing,rest) = exp.partition(_.xs.isEmpty)
    val c = sing.headOption.map{case Prod(a,xs) => Prod(a + b,Nil) }.getOrElse(Prod(b,Nil))
    new LinearModel(c +: rest, vars)
  }
  def -(b: Double): LinearModel = this + (-b)

  def +(that: LinearModel): LinearModel = {
    val invertA = this.exp.map{e => e.xs -> e.a}.toMap
    val invertB = that.exp.map{e => e.xs -> e.a}.toMap
    val keysA = invertA.keySet
    val keysB = invertB.keySet
    val entries = (keysA intersect keysB).map{k => k -> (invertA(k) + invertB(k)) } ++
      (keysA diff keysB).map{k => k -> invertA(k) } ++
      (keysB diff keysA).map{k => k -> invertB(k) }
    new LinearModel(entries.toSeq.collect{case (xs,a) if Math.abs(a) > 1e-10 => Prod(a,xs) }, this.vars ++ that.vars)
  }
  def -(that: LinearModel): LinearModel = this + (-that)

  def <->(that: LinearModel): LinearModel = {
    val invertA = this.exp.map{e => e.xs -> e.a}.toMap
    val invertB = that.exp.map{e => e.xs -> e.a}.toMap
    val keysA = invertA.keySet
    val keysB = invertB.keySet
    val entries = (keysA intersect keysB).map{k => k -> (invertA(k) - invertB(k)) } ++
      (keysA diff keysB).map{k => k -> invertA(k) }
    new LinearModel(entries.toSeq.collect{case (xs,a) if Math.abs(a) > 1e-10 => Prod(a,xs) }, this.vars)
  }

  private def eval(vals: Map[String,Double]): Double = exp.map{_.eval(vals)}.sum
  def eval(vals: (String,Double)*): Double = eval(vals.toMap)

  def partial(vals: (String,Double)*): LinearModel = {
    val vs = vals.toMap
    val prods = exp.map(_.partial(vs))
      .groupBy(_.xs)
      .mapValues(_.reduce((x,y) => Prod(x.a + y.a, x.xs) ))
      .values.toSeq
    val newVars = prods.flatMap(_.xs).toSet
    new LinearModel(prods, newVars)
  }

  def fractional: LinearModel = {
    val prods = exp.filter{case Prod(a,xs) => a > 0 }
    val newVars = prods.flatMap(_.xs).toSet
    new LinearModel(prods, newVars)
  }

  def cleanup: LinearModel = {
    val prods = exp.collect{case Prod(a,xs) if a >= 0 => Prod(Math.round(a),xs) }
      .filter{case Prod(a,xs) => a > 0 }
    val newVars = prods.flatMap(_.xs).toSet
    new LinearModel(prods, newVars)
  }

  override def toString: String = if (exp.isEmpty) "0" else exp.mkString(" + ")
}

object LinearModel {
  def apply(exps: Seq[(Double,String)]): LinearModel = {
    val vars = new mutable.HashSet[String]()
    val prods = exps.collect{
      case (a, xs) if Math.abs(a) > 1e-10 => // HACK: Magic number..
        val vs = xs.split('*').map(_.trim)
        vars ++= vs
        Prod(a, vs)
    }
    new LinearModel(prods, vars.toSet)
  }

  def fromString(x: String): Either[LinearModel,Double] = {
    val vars = new mutable.HashSet[String]()
    val prods = x.split('+').map(_.trim).map{lin =>
      if (lin.isEmpty) {
        Prod(0.0,Nil)
      }
      else {
        val parts = lin.split('*').map(_.trim)
        val (doubles,vs) = parts.partition{x => Try(x.toDouble).isSuccess }
        val a = doubles.map(_.toDouble).product
        vars ++= vs
        Prod(a, vs)
      }
    }
    if (vars.nonEmpty) Left(new LinearModel(prods, vars.toSet)) else Right(prods.map(_.a).sum)
  }
}
