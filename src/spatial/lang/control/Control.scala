package spatial.lang
package control

import argon._
import forge.tags._
import spatial.data._
import spatial.node._

class Directives(options: CtrlOpt) {
  lazy val Foreach   = new ForeachClass(options)
  lazy val Reduce    = new ReduceClass(options)
  lazy val Fold      = new FoldClass(options)
  lazy val MemReduce = new MemReduceClass(options)
  lazy val MemFold   = new MemFoldClass(options)

  @rig protected def unit_pipe(func: => Any): Void = {
    val block = stageBlock{ func; void }
    val pipe = stage(UnitPipe(Set.empty, block))
    options.set(pipe)
    pipe
  }
}

class Pipe(name: Option[String], ii: Option[Int]) extends Directives(CtrlOpt(name,Some(Sched.Pipe),ii)) {
  /** "Pipelined" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)

  def II(ii: Int) = new Pipe(name, Some(ii))
}
class Stream(name: Option[String]) extends Directives(CtrlOpt(name,Some(Sched.Stream))) {
  /** "Streaming" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)

  @api def apply(wild: Wildcard)(func: => Any): Void = Stream.Foreach(*){_ => func }
}
class Sequential(name: Option[String]) extends Directives(CtrlOpt(name,Some(Sched.Seq))) {
  /** "Sequential" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)
}

object Named {
  def apply(name: String) = new NamedClass(name)
}

object Pipe extends Pipe(ii = None, name = None)
object Sequential extends Sequential(name = None)
object Stream extends Stream(name = None)

object Accel extends AccelClass(None)
object Foreach extends ForeachClass(CtrlOpt(None,None,None))
object Reduce extends ReduceClass(CtrlOpt(None,None,None))
object Fold extends FoldClass(CtrlOpt(None,None,None))
object MemReduce extends MemReduceClass(CtrlOpt(None,None,None))
object MemFold extends MemFoldClass(CtrlOpt(None,None,None))
