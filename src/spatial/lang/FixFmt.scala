package spatial.lang

import emul.FixFormat

import scala.collection.mutable

case class FixFmt[A](isSigned: Boolean, ibits: Int, fbits: Int) {
  def nBits: Int = ibits + fbits
  def v: (Boolean,Int,Int) = (isSigned, ibits, fbits)
  def toEmul: FixFormat = FixFormat(isSigned,ibits,fbits)
}

object FixFmt {
  private lazy val fmts = mutable.HashMap[(Boolean,Int,Int),FixFmt[_]]()

  def apply[A:FixFmt]: FixFmt[A] = implicitly[FixFmt[A]]
  def apply[A](s: Boolean, i: Int, f: Int): FixFmt[A] = {
    fmts.getOrElseUpdate((s,i,f), new FixFmt[A](s,i,f)).asInstanceOf[FixFmt[A]]
  }

  implicit def apply[S:BOOL,I:INT,F:INT]: FixFmt[(S,I,F)] = {
    new FixFmt(BOOL[S].v,INT[I].v,INT[F].v).asInstanceOf[FixFmt[(S,I,F)]]
  }
}

