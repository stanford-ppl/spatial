package spatial.metadata

import argon._

package object debug {
  implicit class MemoryDebugOps(s: Sym[_]) {
    def shouldDumpFinal: Boolean = metadata[ShouldDumpFinal](s).exists(_.flag)
    def shouldDumpFinal_=(flag: Boolean): Unit = metadata.add[ShouldDumpFinal](s, ShouldDumpFinal(flag))

    def noWarnWR: Boolean = metadata[NoWarnWriteRead](s).exists(_.flag)
    def noWarnWR_=(flag: Boolean): Unit = metadata.add[NoWarnWriteRead](s, NoWarnWriteRead(flag))
  }
}
