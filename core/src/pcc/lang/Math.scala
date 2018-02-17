package pcc.lang

import forge._
import pcc.core._
import pcc.node._
import pcc.util.Invert._

object Math {
  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a.view[Bits],b.view[Bits]))

  @api def sum[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].zero else reduce(xs:_*){_+_}
  @api def product[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].one else reduce(xs:_*){_*_}

  @api def reduce[T](xs: T*)(reduce: (T,T) => T): T = reduceTreeLevel(xs, reduce).head

  @rig private def reduceTreeLevel[T](xs: Seq[T], reduce: (T,T) => T): Seq[T] = xs.length match {
    case 0 => throw new Exception("Empty reduction level")
    case 1 => xs
    case len if len % 2 == 0 => reduceTreeLevel(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) }, reduce)
    case len => reduceTreeLevel(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) } :+ xs.last, reduce)
  }


}
