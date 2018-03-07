
implicit class SeqOps[A](x: Seq[A]) {

  private def inter(left: Boolean, x: Seq[A], y: Seq[A]): Seq[A] = (left, x, y) match {
    case (_, Nil, Nil) => Nil
    case (true, Nil, _) => y
    case (true, _, _) => x.head +: inter(!left, x.tail, y)
    case (false, _, Nil) => x
    case (false, _, _) => y.head +: inter(!left, x, y.tail)
  }

  def interleave(y: Seq[A]): Seq[A] = inter(left = true, x, y)
}

val x = List(1,2,3)
val y = List(4,5,6,7,8,9)

x.interleave(y)
y.interleave(x)

x.interleave(Nil)
y.interleave(y)

Nil.interleave(Nil)
Nil.interleave(x)
