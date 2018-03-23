package spatial.test

import core._
import spatial.lang._
import utest._


object MatchingTests extends Testbench { val tests = Tests {
  'MatchI32 - {
    val x = I32(32)

    val y = I32(32)

    if (!(x.tp =:= y.tp)) throw new Exception("Types differ!")
    if (!(x.c.get == y.c.get)) throw new Exception("Values differ!")

    x match {
      case Literal(32) => println("Aww yeah")
      case _ => throw new Exception("Nooo")
    }
  }
}}