package spatial.lang
package control

import argon._
import forge.tags._
import spatial.metadata.control._
import spatial.node._

abstract class Directives(options: CtrlOpt) {
  lazy val Foreach   = new ForeachClass(options)
  lazy val Reduce    = new ReduceClass(options)
  lazy val Fold      = new FoldClass(options)
  lazy val MemReduce = new MemReduceClass(options)
  lazy val MemFold   = new MemFoldClass(options)

  @rig protected def unit_pipe(func: => Any, ens: Set[Bit] = Set.empty, stopWhen: Option[Reg[Bit]] = None): Void = {
    val block = stageBlock{ func; void }
    stageWithFlow(UnitPipe(Set.empty, block, stopWhen)){pipe => options.set(pipe) }
  }
}

class Pipe(name: Option[String], ii: Option[Int], directive: Option[UnrollStyle], nobind: Boolean, stopWhen: Option[Reg[Bit]], haltIfStarved: Boolean)
    extends Directives(CtrlOpt(name,Some(Pipelined),ii, stopWhen = stopWhen, mop = directive == Some(MetapipeOfParallels), pom = directive == Some(ParallelOfMetapipes), nobind = nobind, haltIfStarved = haltIfStarved)) {
  /** "Pipelined" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)
  @rig def apply(ens: Set[Bit], func: => Any): Void = unit_pipe(func, ens)
  @rig def apply(breakWhen: Reg[Bit])(func: => Any): Void = unit_pipe(func)

  // TODO: Should the 5th arg be "stopWhen" instead of "None"?
  def II(ii: Int) = new Pipe(name, Some(ii), directive, nobind, None, haltIfStarved)
  def POM = new Pipe(name, ii, Some(ParallelOfMetapipes), nobind, None, haltIfStarved)
  def MOP = new Pipe(name, ii, Some(MetapipeOfParallels), nobind, None, haltIfStarved)
  def NoBind = new Pipe(name, ii, directive, true, None, haltIfStarved)
  def haltIfStarved = new Pipe(name, ii, directive, nobind, None, true)
}
class Stream(name: Option[String], stopWhen: Option[Reg[Bit]], haltIfStarved: Boolean) extends Directives(CtrlOpt(name,Some(Streaming),None,stopWhen,haltIfStarved)) {
  /** "Streaming" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)

  @api def apply(wild: Wildcard)(func: => Any): Void = Stream.Foreach(*){_ => func }

  /** "Stream" controller that will break immediately when `breakWhen` is true.
      It will reset the value in `breakWhen` each time the controller finishes.
      Note that this behavior is discouraged because it can lead to tricky 
      behavior if you are not careful about how you use it.
    */
  @api def apply(breakWhen: Reg[Bit]): Stream = new Stream(name, Some(breakWhen), haltIfStarved)
}
class Sequential(name: Option[String], stopWhen: Option[Reg[Bit]], haltIfStarved: Boolean) extends Directives(CtrlOpt(name,Some(Sequenced),None,stopWhen,haltIfStarved)) {
  /** "Sequential" unit controller */
  @api def apply(func: => Any): Void = unit_pipe(func)

  /** "Sequential" controller that will break immediately when `breakWhen` is true.
      It will reset the value in `breakWhen` each time the controller finishes
    */
  @api def apply(breakWhen: Reg[Bit]): Sequential = new Sequential(name, Some(breakWhen), haltIfStarved)
}

object Named {
  def apply(name: String) = new NamedClass(name)
}

object Pipe extends Pipe(ii = None, name = None, directive = None, nobind = false, stopWhen = None, haltIfStarved = false)
object Sequential extends Sequential(name = None, stopWhen = None, haltIfStarved = false)
object Stream extends Stream(name = None, stopWhen = None, haltIfStarved = false)

object Accel extends AccelClass(None)
object Foreach extends ForeachClass(CtrlOpt(None,None,None,None))
object Reduce extends ReduceClass(CtrlOpt(None,None,None,None))
object Fold extends FoldClass(CtrlOpt(None,None,None,None))
object MemReduce extends MemReduceClass(CtrlOpt(None,None,None,None))
object MemFold extends MemFoldClass(CtrlOpt(None,None,None,None))
