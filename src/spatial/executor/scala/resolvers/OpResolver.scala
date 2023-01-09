package spatial.executor.scala.resolvers

import argon._
import argon.node.Enabled
import emul.FixedPoint
import spatial.executor.scala.{EmulResult, EmulVal, ExecutionState}

import scala.reflect.{ClassTag, classTag}

trait OpResolverBase {
  implicit class EnableUtils(enabled: Enabled[_]) {
    def isEnabled(executionState: ExecutionState): Boolean = {
      enabled.ens.forall(executionState.getValue[emul.Bool](_).value)
    }
  }

  def runBlock(blk: Block[_],
               inputMap: Map[Sym[_], EmulResult],
               execState: ExecutionState): EmulResult = {
    val newState = execState.copy()
    inputMap.foreach {
      case (key, value) => newState.register(key, value)
    }
    blk.stms.foreach(newState.runAndRegister(_))
    newState(blk.result)
  }

  def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    implicit val IR: argon.State = execState.IR
    error(s"Current state: $execState")
    error(s"Did not know how to execute ${stm(sym)}")
    throw new Exception(s"Don't know how to execute ${stm(sym)}")
  }
}

object OpResolver
    extends OpResolverBase
    with DisabledResolver // This way it runs (almost) last
    with MemoryResolver
    with NaryResolver
    with ControlResolver
    with MiscResolver
    with IOResolver
    with HostOpResolver
    with FixResolver
    with FltResolver
    with BitOpResolver
    with FIFOResolver
    with StructOpResolver
