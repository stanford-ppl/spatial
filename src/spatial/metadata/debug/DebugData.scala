package spatial.metadata.debug

import argon._

/** Set of reader symbols for each local memory
  *
  * Getter:  sym.readers
  * Setter:  sym.readers = (Set[ Sym[_] ])
  * Default: empty set
  */
case class ShouldDumpFinal(flag: Boolean) extends Data[ShouldDumpFinal](SetBy.User)

case class NoWarnWriteRead(flag: Boolean) extends Data[NoWarnWriteRead](Transfer.Mirror)

