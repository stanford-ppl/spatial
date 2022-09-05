package spatial.metadata.transform

import argon._

/** Flag that marks whether or not we should dig deeper into the structure for streamification
  *
  * Getter:  sym.streamPrimitive
  * Setter:  sym.streamPrimitive = (true | false)
  * Default: false
  */
case class StreamPrimitive(flag: Boolean) extends Data[StreamPrimitive](Transfer.Mirror)


/**
  * Flag that marks whether or not this memory's banking patterns and such should be analyzed/changed
  * Generally useful for when
  * @param flag
  */
case class FreezeMem(flag: Boolean) extends Data[FreezeMem](Transfer.Mirror)
