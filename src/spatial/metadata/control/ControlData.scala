package spatial.metadata.control

import argon._
import spatial.lang._

/** Control node schedule */
sealed abstract class CtrlSchedule
case object Sequenced extends CtrlSchedule
case object Pipelined extends CtrlSchedule
case object Streaming extends CtrlSchedule
case object ForkJoin extends CtrlSchedule
case object Fork extends CtrlSchedule
case object PrimitiveBox extends CtrlSchedule

/** Transfer type. */
sealed abstract class TransferType
case object DenseStore extends TransferType
case object DenseLoad  extends TransferType
case object SparseStore extends TransferType
case object SparseLoad  extends TransferType

/** Unroll scheme. */
sealed abstract class UnrollStyle
case object MetapipeOfParallels  extends UnrollStyle
case object ParallelOfMetapipes  extends UnrollStyle

/** Control node level. */
sealed abstract class CtrlLevel
case object Inner extends CtrlLevel { override def toString = "InnerControl" }
case object Outer extends CtrlLevel { override def toString = "OuterControl" }

/** Control node looping. */
sealed abstract class CtrlLooping
case object Single extends CtrlLooping
case object Looped extends CtrlLooping

/** IndexCounter and lane info */
case class IndexCounterInfo[A](ctr: Counter[A], lanes: Seq[Int])

/** A controller's level in the control hierarchy. Flag marks whether this is an outer controller.
  *
  * Getter:  sym.rawLevel
  * Setter:  sym.rawLevel = (CtrlLevel)
  * Default: undefined
  */
case class ControlLevel(level: CtrlLevel) extends Data[ControlLevel](SetBy.Flow.Self)

/** A controller, counter, or counterchain's owning controller.
  *
  * Getter:  sym.owner
  * Setter:  sym.owner = (Sym[_])
  * Default: undefined
  */
case class CounterOwner(owner: Sym[_]) extends Data[CounterOwner](SetBy.Flow.Consumer)

/** The control schedule determined by the compiler.
  *
  * Option:  sym.getRawSchedule
  * Getter:  sym.rawSchedule
  * Setter:  sym.rawSchedule = (Sched)
  * Default: undefined
  */
case class ControlSchedule(sched: CtrlSchedule) extends Data[ControlSchedule](SetBy.Flow.Self)


/** The control schedule annotated by the user, if any.
  *
  * Option:  sym.getUserSchedule
  * Getter:  sym.userSchedule
  * Setter:  sym.userSchedule = (Sched)
  * Default: undefined
  */
case class UserScheduleDirective(sched: CtrlSchedule) extends Data[UserScheduleDirective](SetBy.User)


/** Metadata holding a list of children within a controller.
  *
  * Note that the type of a child is Ctrl.Node, not Ctrl, since a child cannot be the Host.
  *
  * Getter:  sym.rawChildren
  * Setter:  sym.rawChildren = (Seq[Ctrl.Node])
  * Default: Nil
  */
case class Children(children: Seq[Ctrl.Node]) extends Data[Children](SetBy.Flow.Self)

/** The controller (Ctrl) parent of a symbol within the controller hierarchy.
  *
  * Operations defined outside Accel always have the Host as their parent.
  * If the parent is not the Host, the id corresponds to the direct parent controller.
  * MemReduce, for example, has several isolated blocks which represent the logical accumulation
  * into the accumulator. The id in Ctrl will give the index into the logical stage for every
  * true future stage (e.g. the accumulation stage) and will be -1 for pseudo-stages.
  *
  * Getter:  sym.parent
  * Setter:  sym.parent = (Ctrl)
  * Default: Host
  */
case class ParentCtrl(parent: Ctrl) extends Data[ParentCtrl](SetBy.Flow.Consumer)

/** The scope (Ctrl) parent of a symbol within the controller hierarchy.
  *
  * The controller id corresponds to the logical stage index of the parent controller.
  * MemReduce, for example, has several isolated blocks which represent the logical accumulation
  * into the accumulator. The id in Controller will always give the index into the logical stage.
  *
  * Getter:  sym.scope
  * Setter:  sym.scope = (Ctrl)
  * Default: Host
  */
case class ScopeCtrl(scope: Scope) extends Data[ScopeCtrl](SetBy.Flow.Consumer)



/** The block ID (Blk) a symbol is defined within the IR.
  *
  * If the parent is not the Host, the id is the raw index into the parent controller's
  * list of .blocks where this symbol is defined.
  * MemReduce, for example, has several isolated blocks which represent the logical accumulation
  * into the accumulator. Blk will give the index into the specific block.
  *
  * Getter:  sym.blk
  * Setter:  sym.blk = (Blk)
  * Default: Host
  */
case class DefiningBlk(blk: Blk) extends Data[DefiningBlk](SetBy.Flow.Consumer)


/** The counter associated with a loop iterator. Only defined for loop iterators.
  *
  * Option:  sym.getCounter
  * Getter:  sym.counter
  * Setter:  sym.counter = (IndexCounterInfo)
  * Default: undefined
  */
case class IndexCounter(info: IndexCounterInfo[_]) extends Data[IndexCounter](SetBy.Analysis.Self)


/** Latency of a given inner pipe body - used for control signal generation.
  *
  * Getter:  sym.bodyLatency
  * Setter:  sym.bodyLatency = (Seq[Double])
  * Default: Nil
  */
case class BodyLatency(latency: Seq[Double]) extends Data[BodyLatency](SetBy.Analysis.Self)


/** Initiation interval of a given controller - used for control signal generation.
  *
  * Getter:  sym.II
  * Setter:  sym.II = (Double)
  * Default: 1.0
  */
case class InitiationInterval(interval: Double) extends Data[InitiationInterval](SetBy.Analysis.Self)


/** User-defined initiation interval of a given controller.
  *
  * Option:  sym.userII
  * Setter:  sym.userII = (Option[Double])
  * Default: None
  */
case class UserII(interval: Double) extends Data[UserII](SetBy.User)

/**Compiler-definied initiation interval of a given controller.
  *
  * Option: sym.compilerII
  * Setter: sym.compilerII = Double
  * Default: None
  */

case class CompilerII(interval: Double) extends Data[CompilerII](SetBy.Analysis.Self)

/** DRAMs which are written in a given controller.
  *
  * Getter: sym.writtenDRAMs
  * Setter: sym.writtenDRAMs = (Set[ Sym[_] ])
  * Default: empty set
  */
case class WrittenDRAMs(drams: Set[Sym[_]]) extends Data[WrittenDRAMs](SetBy.Flow.Consumer)

/** Local memories which are read in a given controller.
  *
  * Getter: sym.readDRAMs
  * Setter: sym.readDRAMs = (Set[ Sym[_] ])
  * Default: empty set
  */
case class ReadDRAMs(drams: Set[Sym[_]]) extends Data[ReadDRAMs](SetBy.Flow.Consumer)

/** Local memories which are written in a given controller.
  *
  * Getter: sym.writtenMems
  * Setter: sym.writtenMems = (Set[ Sym[_] ])
  * Default: empty set
  */
case class WrittenMems(mems: Set[Sym[_]]) extends Data[WrittenMems](SetBy.Flow.Consumer)

/** Local memories which are read in a given controller.
  *
  * Getter: sym.readMems
  * Setter: sym.readMems = (Set[ Sym[_] ])
  * Default: empty set
  */
case class ReadMems(mems: Set[Sym[_]]) extends Data[ReadMems](SetBy.Flow.Consumer)

/** Memories which are read in order to set up a given controller (i.e. setting up counter bounds or enables).
  *
  * Getter: sym.transientReadMems
  * Setter: sym.transientReadMems = (Set[ Sym[_] ])
  * Default: empty set
  */
case class TransientReadMems(mems: Set[Sym[_]]) extends Data[TransientReadMems](SetBy.Analysis.Self)

/** Flag marking whether or not a controller should attempt to bind in parallel with another controlle.
  * i.e. Controllers derived from "sleep(N)" api and that contain breakIf nodes should never be binded
  *
  * Getter: sym.shouldNotBind
  * Setter: sym.shouldNotBind = Boolean
  * Default: false
  */
case class ShouldNotBind(f: Boolean) extends Data[ShouldNotBind](SetBy.Analysis.Self)

/** Flag marking whether to include forward pressure as part of getRetimed / DS stall signal.
  * We always halt if the controller is stalled (output fifo is full).  Whether an empty inbound fifo should also stall the controller
  * or not turns out to be really hard to decide.  For example, if you are moving 2 elements from a fifo to dram at burst granularity, we don't
  * want the controller to stall when the 2 elements are drained but the DMA is waiting for 14 more elements.  However, if we want to deq
  * from a fifo based on some condition that is mostly false and strobes true for one cycle, and the fifo happens to be empty during that cycle, and
  * this condition is retimed by some amount >= 1, we definitely don't want the controller to attempt to deq garbage so we must halt.
  * Yes, it's confusing.  Stream control needs a huge re-design or a new compiler to make it more user friendly and manageable.
  * I THINK the rule is that if you have a controller that reads from a fifo based on some condition that is expected to be false
  * for a while, flip true for a cycle, then go back to false, you want this haltIfStarved flag = true.
  * See unit test "DeqDependencyCtrl" for an example usage.
  *
  * Getter: sym.haltIfStarved
  * Setter: sym.haltIfStarved = Boolean
  * Default: false
  */
case class HaltIfStarved(f: Boolean) extends Data[HaltIfStarved](SetBy.Analysis.Self)


/** Marks top-level streaming controller as one derived from DRAM transfer during blackbox lowering.
  * Used for runtime performance modeling post-blackbox lowering
  *
  * Getter: sym.loweredTransfer
  * Setter: sym.loweredTransfer = TransferType
  * Default: None
  */
case class LoweredTransfer(typ: TransferType) extends Data[LoweredTransfer](SetBy.Analysis.Self)

/** Tracks the size of the last-level counter, for modeling purposes
  *
  * Getter: sym.loweredTransferSize
  * Setter: sym.loweredTransferSize = (length, par)
  * Default: None
  */
case class LoweredTransferSize(info: (Sym[_], Sym[_], Int)) extends Data[LoweredTransferSize](SetBy.Analysis.Self)

/** Identifies whether this controller should be unrolled as MoP or PoM
  *
  * Getter: sym.unrollDirective
  * Setter: sym.unrollDirective = UnrollStyle
  * Default: None
  */
case class UnrollAsPOM(should: Boolean) extends Data[UnrollAsPOM](SetBy.User)
case class UnrollAsMOP(should: Boolean) extends Data[UnrollAsMOP](SetBy.User)

/** 
  * Set on unrolled unit pipes when loops are fully unrolled. Mark how much the loops are unrolled. Used by pir.
  * Getter:  sym.unrollBy
  * Setter:  sym.unrollBy = (int)
  */
case class UnrollBy(par: Int) extends Data[UnrollBy](SetBy.Analysis.Self)

/** 
  * Set by unroller as it unrolling the graph. Mark unrolled nodes with program order on preunrolled
  * nodes
  * Getter:  sym.progorder
  * Setter:  sym.progorder = (int)
  */
case class ProgramOrder(id: Int) extends Data[ProgramOrder](SetBy.Analysis.Self)

case class ConvertToStreamed(flag: Boolean) extends Data[ConvertToStreamed](SetBy.Analysis.Self)
