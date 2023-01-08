package spatial.executor.scala.resolvers
import argon.{Exp, Op, Sym, emit}
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.{EmulResult, EmulUnit, EmulVal, ExecutionState, SimpleEmulVal}
import spatial.node._

import scala.io.Source

case class EmulFile(handle: Source) extends EmulResult {
  def close(): Unit = handle.close()
}

trait IOResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    implicit val ir: argon.State = execState.IR
    sym match {
      case Op(OpenCSVFile(fname, write)) =>
        val name = execState.getValue[String](fname)
        emit(s"Opening CSV: $fname [write = $write]")
        EmulFile(scala.io.Source.fromFile(name))

      case Op(ReadTokens(file, delim)) =>
        val handle = execState(file) match {case EmulFile(handle) => handle }
        val delimiter = execState.getValue[String](delim)

        val values = handle.getLines().flatMap(_.split(delimiter)).toList
        new ScalaTensor[EmulVal[String]](Seq(values.size), None, Some(values.map(str => Some(SimpleEmulVal(str)))))

      case Op(CloseCSVFile(file)) =>
        execState(file) match {case ef:EmulFile => ef.close() }
        EmulUnit(sym)

      case _ => super.run(sym, execState)
    }
  }
}
