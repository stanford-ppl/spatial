package spatial.metadata.transform

import argon._

/** Flag that marks whether or not we should dig deeper into the structure for streamification
  *
  * Getter:  sym.streamPrimitive
  * Setter:  sym.streamPrimitive = (true | false)
  * Default: false
  */
case class StreamPrimitive(flag: Boolean) extends Data[StreamPrimitive](Transfer.Mirror)

