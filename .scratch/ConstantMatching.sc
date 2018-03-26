import argon._
import spatial.lang._

val x = I32(32)

x match {
  case Literal(32) => println("Yerp")
  case _ => println("Nerp")
}