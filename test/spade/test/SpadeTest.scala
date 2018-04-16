package spade.test

abstract class SpadeTest extends argon.DSLTest with spade.SpadeDesign {
  override def runtimeArgs: Args = NoArgs

  def backends = Seq(
    new Backend(
      name = "Null",
      args = "",
      make = "",
      run  = ""
    ) {
      override def parseRunError(line: String): Result = {
        if (line.trim.startsWith("at")) Error(prev)   // Scala exception
        else super.parseRunError(line)
      }
    }
  )

}

