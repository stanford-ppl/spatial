package spatial.lang

import forge.tags._
import forge.math.ReduceTree
import core._
import spatial.node._
import nova.implicits.views._
import nova.util.IntLike

object Math {

  object meta {
    import nova.implicits.intlike._

    @api def product[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) oneI[T] else reduce(xs:_*){_*_}
    @api def sum[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) zeroI[T] else reduce(xs:_*){_+_}
  }

  @api def mux[A:Bits](s: Bit, a: A, b: A): A = stage(Mux(s,a.view[Bits],b.view[Bits]))

  @api def sum[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].zero else reduce(xs:_*){_+_}
  @api def product[T:Num](xs: T*): T = if (xs.isEmpty) tnum[T].one else reduce(xs:_*){_*_}

  @api def reduce[T](xs: T*)(reduce: (T,T) => T): T = ReduceTree(xs:_*)(reduce)
}
