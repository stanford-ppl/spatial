package spatial.data

import argon._
import forge.tags._
import spatial.lang._
import spatial.util._
import spatial.internal.spatialConfig

/** Control node schedule */
sealed abstract class Sched
object Sched {
  case object Seq extends Sched { override def toString = "Sequential" }
  case object Pipe extends Sched { override def toString = "Pipeline" }
  case object Stream extends Sched { override def toString = "Stream" }
  case object Fork extends Sched { override def toString = "Fork" }
  case object ForkJoin extends Sched { override def toString = "ForkJoin" }
}

/** A controller's level in the control hierarchy. Flag marks whether this is an outer controller.
  *
  * Getter:  sym.isOuter
  * Setter:  sym.isOuter = (true | false)
  * Default: undefined
  */
case class ControlLevel(isOuter: Boolean) extends StableData[ControlLevel]

/** A counter or counterchain's owning controller.
  *
  * Getter:  sym.owner
  * Setter:  sym.owner = (Sym[_])
  * Default: undefined
  */
case class CounterOwner(owner: Sym[_]) extends StableData[CounterOwner]


/** The control schedule determined by the compiler.
  *
  * Option:  sym.getSchedule
  * Getter:  sym.schedule
  * Setter:  sym.schedule = (Sched)
  * Default: undefined
  */
case class ControlSchedule(sched: Sched) extends StableData[ControlSchedule]


/** The control schedule annotated by the user, if any.
  *
  * Option:  sym.getUserSchedule
  * Getter:  sym.userSchedule
  * Setter:  sym.userSchedule = (Sched)
  * Default: undefined
  */
case class UserScheduleDirective(sched: Sched) extends StableData[UserScheduleDirective]


/** Metadata holding a list of children within a controller.
  * Note that the type of a child is Controller, not Ctrl, since a child cannot be the Host.
  *
  * Getter:  sym.children
  * Setter:  sym.children = (Seq[Controller])
  * Default: Nil
  */
case class Children(children: Seq[Controller]) extends FlowData[Children]

/** The controller (Ctrl) parent of a symbol within the controller hierarchy.
  * Operations defined outside Accel always have the Host as their parent.
  * If the parent is not the Host, the id corresponds to the logical stage index of the parent controller.
  * MemReduce, for example, has several isolated blocks which represent the logical accumulation
  * into the accumulator. The id in Ctrl will give the index into the logical stage.
  *
  * Getter:  sym.parent
  * Setter:  sym.parent = (Ctrl)
  * Default: Host
  */
case class ParentCtrl(parent: Ctrl) extends FlowData[ParentCtrl]


/** The block a symbol is defined in within the controller hierarchy.
  * If the parent is not the Host, the id is the index within the parent controller's
  * list of blocks where this symbol is defined.
  * MemReduce, for example, has several isolated blocks which represent the logical accumulation
  * into the accumulator. Blk will give the index into the specific block.
  *
  * Getter:  sym.blk
  * Setter:  sym.blk = (Ctrl)
  * Default: Host
  */
case class ParentBlk(blk: Ctrl) extends FlowData[ParentBlk]


/** The counter associated with a loop iterator.
  * Only defined for loop iterators.
  *
  * Option:  sym.getCounter
  * Getter:  sym.counter
  * Setter:  sym.counter = (Counter)
  * Default: undefined
  */
case class IndexCounter(ctr: Counter[_]) extends FlowData[IndexCounter]


/** Latency of a given inner pipe body - used for control signal generation.
  *
  * Getter:  sym.bodyLatency
  * Setter:  sym.bodyLatency = (Seq[Double])
  * Default: Nil
  */
case class BodyLatency(latency: Seq[Double]) extends StableData[BodyLatency]


/** Initiation interval of a given controller - used for control signal generation.
  *
  * Getter:  sym.II
  * Setter:  sym.II = (Double)
  * Default: 1.0
  */
case class InitiationInterval(interval: Double) extends StableData[InitiationInterval]


/** User-defined initiation interval of a given controller.
  *
  * Option:  sym.userII
  * Setter:  sym.userII = (Option[Double])
  * Default: None
  */
case class UserII(interval: Double) extends StableData[UserII]


trait ControlData {
  implicit class ControlDataOps(s: Sym[_]) {
    def getIsOuter: Option[Boolean] = metadata[ControlLevel](s).map(_.isOuter)
    def isOuter: Boolean = getIsOuter.getOrElse{throw new Exception(s"No control level defined for $s") }
    def isOuter_=(flag: Boolean): Unit = metadata.add(s, ControlLevel(flag))

    def getOwner: Option[Sym[_]] = metadata[CounterOwner](s).map(_.owner)
    def owner: Sym[_] = getOwner.getOrElse{throw new Exception(s"Undefined counter owner for $s") }
    def owner_=(owner: Sym[_]): Unit = metadata.add(s, CounterOwner(owner))

    def getSchedule: Option[Sched] = metadata[ControlSchedule](s).map(_.sched)
    def schedule: Sched = getSchedule.getOrElse{ throw new Exception(s"Undefined schedule for $s") }
    def schedule_=(sched: Sched): Unit = metadata.add(s, ControlSchedule(sched))

    def getUserSchedule: Option[Sched] = metadata[UserScheduleDirective](s).map(_.sched)
    def userSchedule: Sched = getUserSchedule.getOrElse{throw new Exception(s"Undefined user schedule for $s") }
    def userSchedule_=(sched: Sched): Unit = metadata.add(s, UserScheduleDirective(sched))

    def parent: Ctrl = metadata[ParentCtrl](s).map(_.parent).getOrElse(Host)
    def parent_=(p: Ctrl): Unit = metadata.add(s, ParentCtrl(p))


    def children: Seq[Controller] = {
      if (!s.isControl) throw new Exception(s"Cannot get children of non-controller.")
      metadata[Children](s).map(_.children).getOrElse(Nil)
    }
    def children_=(cs: Seq[Controller]): Unit = metadata.add(s, Children(cs))


    def blk: Ctrl = metadata[ParentBlk](s).map(_.blk).getOrElse{Host}
    def blk_=(blk: Ctrl): Unit = metadata.add(s, ParentBlk(blk))

    def bodyLatency: Seq[Double] = metadata[BodyLatency](s).map(_.latency).getOrElse(Nil)
    def bodyLatency_=(latencies: Seq[Double]): Unit = metadata.add(s, BodyLatency(latencies))
    def bodyLatency_=(latency: Double): Unit = metadata.add(s, BodyLatency(Seq(latency)))

    @stateful def latencySum: Double = if (spatialConfig.enableRetiming) s.bodyLatency.sum else 0.0

    def II: Double = metadata[InitiationInterval](s).map(_.interval).getOrElse(1.0)
    def II_=(interval: Double): Unit = metadata.add(s, InitiationInterval(interval))

    def userII: Option[Double] = metadata[UserII](s).map(_.interval)
    def userII_=(interval: Option[Double]): Unit = interval.foreach{ii => metadata.add(s, UserII(ii)) }
  }

  implicit class IndexCounterOps[A](i: Num[A]) {
    def getCounter: Option[Counter[A]] = metadata[IndexCounter](i).map(_.ctr.asInstanceOf[Counter[A]])
    def counter: Counter[A] = getCounter.getOrElse{throw new Exception(s"No counter associated with $i") }
    def counter_=(ctr: Counter[_]): Unit = metadata.add(i, IndexCounter(ctr))
  }
}



