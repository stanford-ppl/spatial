package nova.lang

import forge.tags._
import forge.math.ReduceTree
import nova.core._
import nova.node._

object Math {

  object meta {
    import nova.utils.IntLike._

    @api def product[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) oneI[T] else reduce(xs:_*){_*_}
    @api def sum[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) zeroI[T] else reduce(xs:_*){_+_}
  }

  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a.view[Bits],b.view[Bits]))

  @api def sum[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].zero else reduce(xs:_*){_+_}
  @api def product[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].one else reduce(xs:_*){_*_}

  @api def reduce[T](xs: T*)(reduce: (T,T) => T): T = ReduceTree(xs:_*)(reduce)
}
