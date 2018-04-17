package spade

abstract class SpadeTest extends argon.DSLTest with spade.SpadeDesign {
  override def runtimeArgs: Args = NoArgs

  object Arch extends Backend(
    name = "Null",
    args = "",
    make = "",
    run  = ""
  ) {
    override def shouldRun: Boolean = true

    override def parseRunError(line: String): Result = {
      if (line.trim.startsWith("at")) Error(prev)   // Scala exception
      else super.parseRunError(line)
    }
  }

  def backends = Seq(Arch)

}

