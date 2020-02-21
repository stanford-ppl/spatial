package fringe.templates.memory

sealed trait MemType
object BankedSRAMType extends MemType
object BankedSRAMDualReadType extends MemType
object FFType extends MemType
object FIFOType extends MemType
object LIFOType extends MemType
object ShiftRegFileType extends MemType
object LineBufferType extends MemType
object FIFORegType extends MemType