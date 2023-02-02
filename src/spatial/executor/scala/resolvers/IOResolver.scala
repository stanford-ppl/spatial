package spatial.executor.scala.resolvers
import argon.{Exp, Op, Sym, emit}
import emul.{FixedPoint, FixedPointRange}
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.{EmulResult, EmulUnit, EmulVal, ExecutionState, SimpleEmulVal}
import spatial.node._

import java.io.FileWriter
import scala.io.Source

case class EmulFile(handle: Source) extends EmulResult {
  def close(): Unit = handle.close()
}

case class EmulOutputFile(handle: FileWriter) extends EmulResult {
  def close(): Unit = handle.close()
}

trait IOResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = {
    implicit val ir: argon.State = execState.IR
    op match {
      case OpenCSVFile(fname, write) if !write =>
        val name = execState.getValue[String](fname)
        emit(s"Opening CSV: $fname [write = $write]")
        EmulFile(scala.io.Source.fromFile(name))

      case OpenCSVFile(fname, write) if write =>
        val name = execState.getValue[String](fname)
        emit(s"Opening CSV: $fname [write = $write]")
        EmulOutputFile(new FileWriter(name))

      case ReadTokens(file, delim) =>
        val handle = execState(file) match {case EmulFile(handle) => handle }
        val delimiter = execState.getValue[String](delim)

        val values = handle.getLines().flatMap(_.split(delimiter)).toList
        new ScalaTensor[EmulVal[String]](Seq(values.size), None, Some(values.map(str => Some(SimpleEmulVal(str)))))

      case WriteTokens(file, delim, len, token) =>
        val handle = execState(file) match {case EmulOutputFile(handle) => handle }
        val delimiter = execState.getValue[String](delim)
        val length = execState.getValue[FixedPoint](len).toInt
        (0 until length).foreach {
          i =>
            val text = runBlock(token, Map(token.input -> SimpleEmulVal(FixedPoint.fromInt(i))), execState)
            text match {
              case s: EmulVal[String] =>
                handle.write(s.value)
                handle.write(delimiter)
            }
        }
        EmulUnit(sym)

      case CloseCSVFile(file) =>
        execState(file) match {
          case ef:EmulFile => ef.close()
          case ef:EmulOutputFile => ef.close()
        }
        EmulUnit(sym)

      case _ => super.run(sym, op, execState)
    }
  }
}
