package spatial.metadata.transform

import argon._

/** Flag set by the user to allow buffered writes across metapipeline stages.
  *
  * Getter:  sym.streamify
  * Setter:  sym.streamify = (true | false)
  * Default: false
  */
case class Streamify(flag: Boolean, reason: Option[String] = None) extends Data[Streamify](Transfer.Remove)

