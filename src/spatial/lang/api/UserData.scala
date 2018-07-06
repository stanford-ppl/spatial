package spatial.lang.api

import argon._
import spatial.metadata.bounds._

trait UserData { this: Implicits =>

  object bound {
    def update[A:Type](x: A, bound: Int): Unit = box(x).bound = UpperBound(bound)
  }

}
