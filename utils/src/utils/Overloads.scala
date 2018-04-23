package utils

trait Overloads {
  import Overloads._
  implicit def overload0: Overload0 = new Overload0
  implicit def overload1: Overload1 = new Overload1
  implicit def overload2: Overload2 = new Overload2
  implicit def overload3: Overload3 = new Overload3
  implicit def overload4: Overload4 = new Overload4
  implicit def overload5: Overload5 = new Overload5
  implicit def overload6: Overload6 = new Overload6
}

object Overloads {
  class Overload0
  class Overload1
  class Overload2
  class Overload3
  class Overload4
  class Overload5
  class Overload6
}

