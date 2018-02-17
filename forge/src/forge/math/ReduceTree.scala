package forge.math

object ReduceTree {

  private def levels[T](xs: Seq[T], reduce: (T,T) => T): Seq[T] = xs.length match {
    case 0 => throw new Exception("Empty reduction level")
    case 1 => xs
    case len if len % 2 == 0 => levels(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) }, reduce)
    case len => levels(List.tabulate(len/2){i => reduce( xs(2*i), xs(2*i+1)) } :+ xs.last, reduce)
  }

  def apply[T](xs: T*)(reduce: (T,T) => T): T = levels(xs, reduce).head
}
