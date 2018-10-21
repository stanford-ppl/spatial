package utils.math

object ReduceTree {

  private def levels[T](xs: Seq[T], reduce: (T,T) => T): Seq[T] = xs.length match {
    case 0 => throw new Exception("Empty reduction level")
    case 1 => xs
    case len if len % 2 == 0 => levels(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) }, reduce)
    case len => levels(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) } :+ xs.last, reduce)
  }

  def apply[T](xs: T*)(reduce: (T,T) => T): T = levels(xs, reduce).head
}

/*  Generates all combinations choosing one element from each Seq, with
  *    CSE built in to reuse as much as possible
  *    For example, AllPairsMapTree(Seq( Seq(A,A'), Seq(B,B'), Seq(C,C'), Seq(D,D') ))(_*_)
  *                              = ABCD + ABCD' + ABC'D + ABC'D' + AB'CD + AB'CD' + AB'C'D + AB'C'D' 
  *                                 + A'BCD + A'BCD' + A'BC'D + A'BC'D' + A'B'CD + A'B'CD' + A'B'C'D + A'B'C'D'
  *          
  *          		  (A A')   (B B')      (C C')    (D D')         =   4 Seqs of size 2
  *          
  *          	   (AB  AB'  A'B  A'B')  (CD  CD'  C'D  C'D')       =   2 Seqs of size 4
  *          
  *            (ABCD ABCD' ABC'D ABC'D' AB'CD AB'CD' AB'C'D ... etc) =   1 Seq of size 16
  *
  */
object CombinationTree {

  private def join[T](x1: Seq[T], x2: Seq[T], func: (T,T) => T): Seq[T] = x1.flatMap{a => x2.map{b => func(a,b)}}

  private def combine[T](xs: Seq[Seq[T]], func: (T,T) => T): Seq[Seq[T]] = xs.length match {
    case 0 => throw new Exception("Empty reduction level")
    case 1 => xs
    case len if len % 2 == 0 => combine(List.tabulate(len/2){i => join(xs(2*i), xs(2*i+1), func)}, func)
    case len => combine(List.tabulate(len/2){i => join(xs(2*i), xs(2*i+1), func) } :+ xs.last, func)
  }

  def apply[T](xs: Seq[T]*)(func: (T,T) => T): Seq[T] = combine(xs, func).head
}
