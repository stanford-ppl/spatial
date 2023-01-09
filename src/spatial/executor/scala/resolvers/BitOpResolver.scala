package spatial.executor.scala.resolvers
import argon._
import argon.node._
import emul._
import spatial.executor.scala._

trait BitOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): SomeEmul = sym match {
    case Op(BitToText(a)) =>
      SimpleEmulVal(execState.getValue[emul.Bool](a).toString)
    case Op(BitRandom(None)) =>
      SimpleEmulVal(Bool(scala.util.Random.nextBoolean()))
    case Op(TextToBit(text)) =>
      val str = execState.getValue[String](text)
      SimpleEmulVal(Bool.from(str))
    case _ => super.run(sym, execState)
  }
}
