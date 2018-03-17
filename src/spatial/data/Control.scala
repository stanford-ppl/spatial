package spatial.data

import core._
import forge.tags._
import spatial.lang._
import spatial.util._
import utils.Tree

/** A scheduling directive specified by the user. */
sealed abstract class UserSchedule
object UserSchedule {
  case object Seq extends UserSchedule
  case object Pipe extends UserSchedule
  case object Stream extends UserSchedule
}

/** Scheduling determined by the compiler. */
sealed abstract class ControlSchedule
object ControlSchedule {
  case object Seq extends ControlSchedule { override def toString = "Sequential" }
  case object Pipe extends ControlSchedule { override def toString = "Pipeline" }
  case object Stream extends ControlSchedule { override def toString = "Stream" }
  case object Fork extends ControlSchedule { override def toString = "Fork" }
  case object ForkJoin extends ControlSchedule { override def toString = "ForkJoin" }
}

/** The level of control within the hierarchy. */
sealed abstract class ControlLevel
case object InnerControl extends ControlLevel
case object OuterControl extends ControlLevel

sealed abstract class Ctrl {
  def s: Option[Sym[_]] = None
  def id: Int
  def parent: Ctrl
  def ancestors: Seq[Ctrl] = Tree.ancestors(this){_.parent}
  def ancestors(stop: Ctrl => Boolean): Seq[Ctrl] = Tree.ancestors(this, stop){_.parent}
  def ancestors(stop: Ctrl): Seq[Ctrl] = Tree.ancestors[Ctrl](this, {c => c == stop}){_.parent}
  @stateful def children: Seq[Ctrl]
}
case class Parent(sym: Sym[_], id: Int) extends Ctrl {
  override def s: Option[Sym[_]] = Some(sym)
  def parent: Ctrl = if (id != -1) Parent(sym,-1) else sym.parent
  @stateful def children: Seq[Ctrl] = sym.children
}
case object Host extends Ctrl {
  def id: Int = 0
  def parent: Ctrl = Host
  @stateful def children: Seq[Ctrl] = hwScopes.all
}

/** A controller's level in the control hierarchy. */
case class CtrlLevel(level: ControlLevel) extends StableData[CtrlLevel]
object levelOf {
  def get(x: Sym[_]): Option[ControlLevel] = metadata[CtrlLevel](x).map(_.level)
  def apply(x: Sym[_]): ControlLevel = levelOf.get(x).getOrElse{throw new Exception(s"Undefined control level for $x") }
  def update(x: Sym[_], level: ControlLevel): Unit = metadata.add(x, CtrlLevel(level))
}
object isOuter {
  def apply(x: Sym[_]): Boolean = levelOf(x) == OuterControl
  def update(x: Sym[_], isOut: Boolean): Unit = if (isOut) levelOf(x) = OuterControl else levelOf(x) = InnerControl
}

/** The control schedule determined by the compiler. */
case class ControlScheduling(sched: ControlSchedule) extends StableData[ControlScheduling]
object styleOf {
  def get(x: Sym[_]): Option[ControlSchedule] = metadata[ControlScheduling](x).map(_.sched)
  def apply(x: Sym[_]): ControlSchedule = styleOf.get(x).getOrElse{throw new Exception(s"Undefined schedule for $x")}
  def update(x: Sym[_], sched: ControlSchedule): Unit = metadata.add(x, ControlScheduling(sched))
}

/** The control schedule annotated by the user, if any. */
case class SchedulingDirective(sched: UserSchedule) extends StableData[SchedulingDirective]
object userStyleOf {
  def get(x: Sym[_]): Option[UserSchedule] = metadata[SchedulingDirective](x).map(_.sched)
  def apply(x: Sym[_]): UserSchedule = userStyleOf.get(x).getOrElse{throw new Exception(s"Undefined user schedule for $x") }
  def update(x: Sym[_], sched: UserSchedule): Unit = metadata.add(x, SchedulingDirective(sched))
}


/** Metadata holding a list of children within a controller. */
case class Children(children: Seq[Parent]) extends FlowData[Children]

/** Metadata holding the parent of a controller within the controller hierarchy. */
case class ParentController(parent: Ctrl) extends FlowData[ParentController]

/** Metadata holding the counter associated with a loop iterator. */
case class IndexCounter(ctr: Counter[_]) extends FlowData[IndexCounter]
object ctrOf {
  def get[A](i: Num[A]): Option[Counter[A]] = {
    metadata[IndexCounter](i).map(_.ctr.asInstanceOf[Counter[A]])
  }
  def apply[A](i: Num[A]): Counter[A] = {
    ctrOf.get(i).getOrElse{throw new Exception(s"No counter associated with $i") }
  }
  def update(i: Num[_], ctr: Counter[_]): Unit = metadata.add(i, IndexCounter(ctr))
}


/** All accelerator scopes in the program **/
case class AccelScopes(scopes: Seq[Parent]) extends FlowData[AccelScopes]
@data object hwScopes {
  def all: Seq[Parent] = globals[AccelScopes].map(_.scopes).getOrElse(Nil)
}
