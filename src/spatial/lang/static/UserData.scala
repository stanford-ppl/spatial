package spatial.lang
package static

import argon._
import spatial.data._

trait UserData {

  object bound {
    def update[A:Type](x: A, bound: Int): Unit = boundOf(x) = UpperBound(bound)
  }


}
