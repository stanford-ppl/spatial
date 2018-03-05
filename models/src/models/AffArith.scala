package models

trait AffArith[T] {
  def plus(x: T, y: T): T
  def minus(x: T, y: T): T
  def times(x: T, y: Double): T
  def div(x: T, y: Double): T
}

object AffArith {
  implicit object DoubleIsArith extends AffArith[Double] {
    override def plus(x: Double, y: Double): Double = x + y
    override def minus(x: Double, y: Double): Double = x - y
    override def times(x: Double, y: Double): Double = x * y
    override def div(x: Double, y: Double): Double = x / y
  }
  implicit object LinearModelIsArith extends AffArith[LinearModel] {
    override def plus(x: LinearModel, y: LinearModel): LinearModel =  x + y
    override def minus(x: LinearModel, y: LinearModel): LinearModel = x - y
    override def times(x: LinearModel, y: Double): LinearModel = x * y
    override def div(x: LinearModel, y: Double): LinearModel = x / y
  }
}
