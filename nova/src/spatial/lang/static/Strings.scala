package spatial.lang.static

import forge.tags._

import scala.collection.immutable.{StringOps, WrappedString}

// Side note, I can't believe this actually works
trait StringSuperLowImplicits {
  implicit def wrapStringUnstaged(x: String): WrappedString = new WrappedString(x)
}

trait StringVeryLowImplicits extends StringSuperLowImplicits {
  implicit def augmentStringUnstaged(x: String): StringOps = new StringOps(x)
}

trait StringLowPriorityImplicits extends StringVeryLowImplicits {
  // Shadows Predef method..
  @api implicit def wrapString(x: String): Text = Text.c(x)
}

trait Strings extends StringLowPriorityImplicits {
  // Shadows Predef method...
  @api implicit def augmentString(x: String): Text = Text.c(x)
}
