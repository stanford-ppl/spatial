package spatial.executor.scala.resolvers
import argon.{Exp, Op, Sym}
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.{EmulResult, ExecutionState, EmulUnit}
import spatial.node._

import scala.io.Source

case class EmulFile(sym: Sym[_], handle: Source) extends EmulResult

trait IOResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(OpenCSVFile(fname, write)) =>
      val name = execState.getValue[String](fname)
      execState.log(s"Opening CSV: $fname [write = $write]")
      EmulFile(sym, scala.io.Source.fromFile(name))

    case Op(ReadTokens(file, delim)) =>
      val handle = execState(file) match {case EmulFile(_, handle) => handle }
      val delimiter = execState.getValue[String](delim)

      val values = handle.getLines().flatMap(_.split(delimiter)).toList
      new ScalaTensor[String](sym, Seq(values.size), Some(values.map(Some(_))))

    case Op(CloseCSVFile(file)) =>
      val handle = execState(file) match {case EmulFile(_, handle) => handle }
      handle.close()
      EmulUnit(sym)

    case _ => super.run(sym, execState)
  }
}
