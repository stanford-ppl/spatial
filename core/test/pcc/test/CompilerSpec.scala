/*package pcc.test

import org.scalatest.{FlatSpec, Matchers}
import pcl.compiler.{Compiler, ParserError, SrcCtx}

class CompilerSpec extends FlatSpec with Matchers {

  val validCode =
    """
      |read input name, country
      |switch:
      |  country == "PT" ->
      |    call service "A"
      |    exit
      |  otherwise ->
      |    call service "B"
      |    switch:
      |      name == "unknown" ->
      |        exit
      |      otherwise ->
      |        call service "C"
      |        exit
    """.stripMargin.trim

  val invalidCode =
    """
      |read input name, country
      |switch:
      |  country == PT ->
      |    call service "A"
      |    exit
      |  otherwise ->
      |    call service "B"
      |    switch:
      |      name == "unknown" ->
      |        exit
      |      otherwise ->
      |        call service "C"
      |        exit
    """.stripMargin.trim

  val successfulAST = AndThen(
    ReadInput(List("name", "country")),
    Choice(List(
      IfThen( Equals("country", "PT"), AndThen(CallService("A"), Exit) ),
      OtherwiseThen(
        AndThen(
          CallService("B"),
          Choice(List(
            IfThen( Equals("name", "unknown"), Exit ),
            OtherwiseThen( AndThen(CallService("C"), Exit) )
          ))
        )
      )
    ))
  )

  val errorMsg = ParserError(SrcCtx(3,14), "string literal expected")


  "Workflow compiler" should "successfully parse a valid workflow" in {
    Compiler(validCode) shouldBe Right(successfulAST)
  }

  it should "return an error with an invalid workflow" in {
    Compiler(invalidCode) shouldBe Left(errorMsg)
  }
}*/
