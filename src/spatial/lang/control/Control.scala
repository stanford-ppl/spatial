package spatial.lang
package control

import core._
import forge.tags._
import spatial.data._
import spatial.node._

class Directives(options: CtrlOpt) extends ForeachClass(options) {
  lazy val Foreach   = new ForeachClass(options)
  lazy val Reduce    = new ReduceClass(options)
  lazy val Fold      = new FoldClass(options)
  lazy val MemReduce = new MemReduceClass(options)
  lazy val MemFold   = new MemFoldClass(options)

  @rig protected def unit_pipe(func: => Void): Void = {
    val block = stageBlock{ func }
    val pipe = stage(UnitPipe(block, ens = Set.empty))
    options.set(pipe)
    pipe
  }
}

class Pipe(name: Option[String], ii: Option[Int]) extends Directives(CtrlOpt(name,Some(MetaPipe),ii)) {
  /** "Pipelined" unit controller **/
  @api def apply(func: => Void): Void = unit_pipe(func)

  def apply(ii: Int) = new Pipe(name, Some(ii))
}
class Stream(name: Option[String]) extends Directives(CtrlOpt(name,Some(StreamPipe))) {
  /** "Streaming" unit controller **/
  @api def apply(func: => Void): Void = unit_pipe(func)
}
class Sequential(name: Option[String]) extends Directives(CtrlOpt(name,Some(SeqPipe))) {
  /** "Sequential" unit controller **/
  @api def apply(func: => Void): Void = unit_pipe(func)
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
