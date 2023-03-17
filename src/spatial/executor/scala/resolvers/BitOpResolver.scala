package spatial.executor.scala.resolvers
import argon._
import argon.node._
import emul._
import spatial.executor.scala._

trait BitOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): SomeEmul = op match {
    case BitToText(a) =>
      SimpleEmulVal(execState.getValue[emul.Bool](a).toString)
    case BitRandom(None) =>
      SimpleEmulVal(Bool(scala.util.Random.nextBoolean()))
    case TextToBit(text) =>
      val str = execState.getValue[String](text)
      SimpleEmulVal(Bool.from(str))
    case _ => super.run(sym, op, execState)
  }
}
