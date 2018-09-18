package fringe.utils

object Banks {
  /* List of bank addresses, for direct accesses */
  type Banks = List[Int]
  def Banks(xs: Int*) = List(xs:_*)
}
