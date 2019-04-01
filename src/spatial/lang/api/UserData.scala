package spatial.lang.api

import argon._
import forge.tags.stateful
import spatial.metadata.bounds._

trait UserData { this: Implicits =>

  object bound {
    @stateful def update[A:Type](x: A, bound: Int): Unit = box(x).bound = UpperBound(bound)
  }

}
