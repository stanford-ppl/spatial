package utils

package object math {
  def log2(x: Double): Double = Math.log10(x)/Math.log10(2)
  def isPow2(x: Int): Boolean = (x & (x-1)) == 0
  
  // TODO: fix scaladoc here
  /* Find all combinations out of list of lists.  i.e.
    *   combs(List( List(a,b),
    *               List(x,y,z),
    *               List(m,n)
    *              )
    *         )
    *         =
    *         List( List(a,x,m), List(a,x,n), List(a,y,m), List(a,y,n), ...)
    */
  def combs[T](lol: List[List[T]]): List[List[T]] = lol match {
      case Nil => List(Nil)
      case l::rs => for(x <- l;cs <- combs(rs)) yield x::cs
    }
}
