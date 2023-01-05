package spatial.executor.scala.resolvers

import argon._
import spatial.executor.scala.{EmulResult, ExecutionState}

trait OpResolverBase {
  def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    execState.log(s"Current state: $execState")
    throw new NotImplementedError(s"Don't know how to execute ${stm(sym)}")
  }
}

object OpResolver extends OpResolverBase with MemoryResolver with NaryResolver with ControlResolver with MiscResolver with IOResolver with HostOpResolver
