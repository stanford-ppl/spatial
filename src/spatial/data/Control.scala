package spatial.data

import core._
import spatial.lang._

/** A scheduling directive specified by the user. */
sealed abstract class UserSchedule
case object SeqPipe extends UserSchedule
case object InnerPipe extends UserSchedule
case object MetaPipe extends UserSchedule
case object StreamPipe extends UserSchedule

/** The level of control within the hierarchy. */
sealed abstract class ControlLevel
case object InnerControl extends ControlLevel
case object OuterControl extends ControlLevel

case class Ctrl(sym: Sym[_], id: Int)

/** Metadata holding a controller's level in the control hierarchy. */
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


/** Metadata holding the control schedule annotated by the user, if any. */
case class AnnotatedSchedule(sched: UserSchedule) extends StableData[AnnotatedSchedule]
object userStyleOf {
  def get(x: Sym[_]): Option[UserSchedule] = metadata[AnnotatedSchedule](x).map(_.sched)
  def apply(x: Sym[_]): UserSchedule = userStyleOf.get(x).getOrElse{throw new Exception(s"Undefined user schedule for $x") }
  def update(x: Sym[_], sched: UserSchedule): Unit = metadata.add(x, AnnotatedSchedule(sched))
}


/** Metadata holding a list of children within a controller. */
case class Children(children: Seq[Sym[_]]) extends FlowData[Children]
object childrenOf {
  def apply(x: Sym[_]): Seq[Sym[_]] = metadata[Children](x).map(_.children).getOrElse(Nil)
  def update(x: Sym[_], children: Seq[Sym[_]]): Unit = metadata.add(x, Children(children))
}


/** Metadata holding the parent of a controller within the controller hierarchy. */
case class Parent(parent: Ctrl) extends FlowData[Parent]
object parentOf {
  def get(x: Sym[_]): Option[Ctrl] = metadata[Parent](x).map(_.parent)
  def apply(x: Sym[_]): Ctrl = parentOf.get(x).getOrElse{throw new Exception(s"Undefined parent for $x") }
  def update(x: Sym[_], parent: Ctrl): Unit = metadata.add(x, Parent(parent))
}

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
