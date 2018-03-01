package spatial.lang

import emul.FltFormat

import scala.collection.mutable

case class FltFmt[A](mbits: Int, ebits: Int) {
  def nBits: Int = mbits + ebits
  def v: (Int,Int) = (mbits, ebits)
  def toEmul: FltFormat = FltFormat(mbits-1,ebits)
}

object FltFmt {
  private lazy val fmts = mutable.HashMap[(Int,Int),FltFmt[_]]()

  def apply[A:FltFmt]: FltFmt[A] = implicitly[FltFmt[A]]
  def apply[A](m: Int, e: Int): FltFmt[_] = {
    fmts.getOrElseUpdate((m,e), new FltFmt[A](m,e)).asInstanceOf[FltFmt[A]]
  }

  implicit def apply[M:INT,E:INT]: FltFmt[(M,E)] = {
    new FltFmt(INT[M].v,INT[E].v).asInstanceOf[FltFmt[(M,E)]]
  }
}

