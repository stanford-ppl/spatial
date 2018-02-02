package pcc.data

import pcc.core._
import pcc.lang._

case class Prod(xs: Seq[I32], m: Int = 1) {
  def isConst: Boolean = xs.isEmpty
  def unary_-(): Prod = Prod(xs, -m)
  def *(p: Prod): Prod = Prod(this.xs ++ p.xs, this.m * p.m)
  def +(p: Prod): Sum = Sum(Seq(this,p))
  def -(p: Prod): Sum = Sum(Seq(this,-p))

  def *(s: Sum): Sum = s * this
  def +(s: Sum): Sum = s + this
  def -(s: Sum): Sum = -s + this

  def *(c: Int): Option[Prod] = if (c != 0) Some(Prod(xs, m * c)) else None
  def +(c: Int): Sum  = Sum(Seq(this), c)
  def -(c: Int): Sum  = Sum(Seq(this), c)
}
object One extends Prod(Nil,1)

case class Sum(ps: Seq[Prod], b: Int = 0) {
  def isConst: Boolean = ps.isEmpty
  def unary_-(): Sum = Sum(ps.map{p => -p }, -b)
  def *(s: Sum): Sum
  def +(s: Sum): Sum = Sum(this.ps ++ s.ps, this.b + s.b)
  def -(s: Sum): Sum = -s + this

  def *(that: Prod): Sum = Sum(this.ps.map{_ * that} ++ , )
}
object Zero extends Prod(Nil,0)


class AccessPatterns {

}
