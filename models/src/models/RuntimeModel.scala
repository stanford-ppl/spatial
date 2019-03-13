package models

import java.io.File
import java.io.PrintWriter

import scala.io.Source


object Runtime {

  var interactive = true
  var retune = false
  var currentAsk = 0
  var logfilename: String = ""
  var logfile: Option[PrintWriter] = None
  var cliParams = Seq[Int]()
  var tuneParams = Map[Int, Any]()
  var isFinal = false
  var suppressWarns = false

  /** Asked values mapping */
  // Mapping between Ask ids and their values
  val askMap = scala.collection.mutable.Map[Int, Int]()
  // Asked values for this execution
  val cachedAsk = scala.collection.mutable.ListBuffer[Int]()
  // Tuned values for this execution
  val cachedTune = scala.collection.mutable.ListBuffer[Int]()

  /** Control node schedule */
  sealed abstract class CtrlSchedule
  case object Sequenced  extends CtrlSchedule
  case object Pipelined  extends CtrlSchedule
  case object Streaming  extends CtrlSchedule
  case object ForkJoin   extends CtrlSchedule
  case object Fork       extends CtrlSchedule
  case object DenseStore extends CtrlSchedule // modeled as a schedule
  case object GatedDenseStore extends CtrlSchedule // modeled as a schedule, used for unaligned stores 
  case object DenseLoad  extends CtrlSchedule // modeled as a schedule
  case object SparseStore extends CtrlSchedule // modeled as a schedule
  case object SparseLoad  extends CtrlSchedule // modeled as a schedule


  /** Control node level. */
  sealed abstract class CtrlLevel
  case object InnerControl extends CtrlLevel
  case object OuterControl extends CtrlLevel

  /** Structure for tracking who competes with given transfer */
  case class Competitors(loads: Int, stores: Int, gateds: Int) {
    def toSeq: Seq[Int] = Seq(loads, stores, gateds)
    def +(b: Competitors): Competitors = Competitors(loads + b.loads, stores + b.stores, gateds + b.gateds)
    def *(b: Int): Competitors = Competitors(loads*b, stores*b, gateds*b)
  }
  object Competitors {
    def empty = Competitors(0,0,0)
    def DenseLoad = Competitors(1,0,0)
    def DenseStore = Competitors(0,1,0)
    def GatedDenseStore = Competitors(0,0,1)
  }

  /** Open logfile */
  def begin(file: String): Unit = {
    logfile = Some(new PrintWriter(new File(file)))
    logfilename = file
  }

  /** Close logfile */
  def end(): Unit = logfile.get.close()

  /** Tee string to both logfile and stdout */
  def emit(x: String): Unit = {
    println(x)
    logfile.get.write(x + "\n")
  }

  /** Info about node */ 
  case class Ctx(
    val id: String, // Sym name (i.e. x####)
    val line: String, // Line number of item
    val info: String,  // Code for this node (i.e. "Foreach(N){i => " )
    val stm: String // IR node from spatial
  ){
    override def toString: String = s"line $line: $id"
  }
  object Ctx {
    def empty: Ctx = Ctx("??","??","","")
  }

  /** Base class for numbers used in model (parallelizations, counter starts/steps/ends, tile sizes, etc.) */
  abstract class ModelValue(id: Int, whatAmI: String, ctx: Ctx){
    def lookup: Int
  }

  /** Value that can be hot-swapped by compiler during DSE */
  case class Tuneable(id: Int, val default: Int, whatAmI: String) extends ModelValue(id, whatAmI, Ctx.empty) {
    def lookup: Int = {
      if (!suppressWarns) {
        if (!tuneParams.contains(id) && !cachedTune.contains(id)) {println(s"[warn] Using default value $default for tuneable param $whatAmI ($id)"); cachedTune += id}
        else if (!cachedTune.contains(id)) {println(s"[warn] Using retuned value ${tuneParams(id)} for tuneable param $whatAmI ($id) (default was $default)"); cachedTune += id}
      }
      // println(s"looking up $id default $default in params $tuneParams")
      tuneParams.getOrElse(id, default).asInstanceOf[Int]
    }
  }

  /** Value that is a constant from point of view of compiler and user */
  case class Locked(id: Int, val value: Int) extends ModelValue(id, "", Ctx.empty) {
    def lookup: Int = value
  }

  /** Value that must be set by user at command line, or dseModelArgs/finalModelArgs in noninteractive mode */
  case class Ask(id: Int, whatAmI: String, ctx: Ctx) extends ModelValue(id, whatAmI, ctx) {
    def lookup: Int = {
      if (cachedAsk.contains(id)) askMap(id)
      else if (!interactive && !cachedAsk.contains(id)) {
        val t = if (cliParams.size < currentAsk) {println(s"asking for param $currentAsk: ${cliParams(currentAsk)}"); cliParams(currentAsk)}
                else {println(s"[WARNING] Param $currentAsk not provided! Using value ${askMap.getOrElse(id, 1)}"); askMap.getOrElse(id, 1)}
        currentAsk = currentAsk + 1
        askMap += (id -> t)
        cachedAsk += id
        t
      }
      else if (!interactive && cachedAsk.contains(id)) {
        askMap(id)
      }
      else {
        val default = askMap.getOrElse(id, 1)
        print(s"Value for $whatAmI (${ctx.toString}) [default: $default] : ")
        val t = scala.io.StdIn.readLine().trim()
        val x = if (t != "") t.toInt else default
        println("")
        askMap += (id -> x)
        cachedAsk += id
        x
      }
    }
  }

  object CtrModel {
    def apply(start: Int, stop: Int, stride: Int, par: Int) = new CtrModel(Locked(-1, start), Locked(-1, stop), Locked(-1, stride), Locked(-1, par))
    def apply(start: Int, stop: Int, stride: Int, par: ModelValue) = new CtrModel(Locked(-1, start), Locked(-1, stop), Locked(-1, stride), par)
    def apply(start: Int, stop: Int, stride: ModelValue, par: Int) = new CtrModel(Locked(-1, start), Locked(-1, stop), stride, Locked(-1, par))
    def apply(start: Int, stop: Int, stride: ModelValue, par: ModelValue) = new CtrModel(Locked(-1, start), Locked(-1, stop), stride, par)
    def apply(start: Int, stop: ModelValue, stride: Int, par: Int) = new CtrModel(Locked(-1, start), stop, Locked(-1, stride), Locked(-1, par))
    def apply(start: Int, stop: ModelValue, stride: Int, par: ModelValue) = new CtrModel(Locked(-1, start), stop, Locked(-1, stride), par)
    def apply(start: Int, stop: ModelValue, stride: ModelValue, par: Int) = new CtrModel(Locked(-1, start), stop, stride, Locked(-1, par))
    def apply(start: Int, stop: ModelValue, stride: ModelValue, par: ModelValue) = new CtrModel(Locked(-1, start), stop, stride, par)
    def apply(start: ModelValue, stop: Int, stride: Int, par: Int) = new CtrModel(start, Locked(-1, stop), Locked(-1, stride), Locked(-1, par))
    def apply(start: ModelValue, stop: Int, stride: Int, par: ModelValue) = new CtrModel(start, Locked(-1, stop), Locked(-1, stride), par)
    def apply(start: ModelValue, stop: Int, stride: ModelValue, par: Int) = new CtrModel(start, Locked(-1, stop), stride, Locked(-1, par))
    def apply(start: ModelValue, stop: Int, stride: ModelValue, par: ModelValue) = new CtrModel(start, Locked(-1, stop), stride, par)
    def apply(start: ModelValue, stop: ModelValue, stride: Int, par: Int) = new CtrModel(start, stop, Locked(-1, stride), Locked(-1, par))
    def apply(start: ModelValue, stop: ModelValue, stride: Int, par: ModelValue) = new CtrModel(start, stop, Locked(-1, stride), par)
    def apply(start: ModelValue, stop: ModelValue, stride: ModelValue, par: Int) = new CtrModel(start, stop, stride, Locked(-1, par))    
    def apply(start: ModelValue, stop: ModelValue, stride: ModelValue, par: ModelValue) = new CtrModel(start, stop, stride, par)    
  }
  class CtrModel(
    val start: ModelValue,
    val stop: ModelValue, 
    val stride: ModelValue,
    val par: ModelValue
  ) {
    def N: Int = {
      val realStart = start.lookup
      val realStop = stop.lookup
      val realStride = stride.lookup
      val realPar = par.lookup
      /** Round n up to the nearest multiple of t */
      def roundUp(n: Int, t: Int): Int = {
        if (n == 0) 0 else {((n + t - 1).toDouble / t.toDouble).toInt * t}
      }
      roundUp(scala.math.ceil((realStop - realStart).toDouble / realStride.toDouble).toInt, realPar) / realPar
    }
  }

  case class CChainModel(
    val ctrs: Seq[CtrModel],
    val ctx: Ctx = Ctx("","","","")
  ) {
    def N: Int = { // Num iters for lane
      if (ctrs.isEmpty) 1 
      else ctrs.map(_.N).product
    }
    def unroll: Int = { 
      if (isFinal) 1 else ctrs.map(_.par.lookup).product
    }
    def isDefined: Boolean = !ctrs.isEmpty
  }

  class ControllerModel(
    val id: Int,
    val level: CtrlLevel,
    val schedule: CtrlSchedule,
    val cchain: List[CChainModel],
    val L: Int,
    val II: Int,
    val ctx: Ctx,
    val bitsPerCycle: Double = 32.0 // Defined for transfers
  ){
    def this(id: Int, level: CtrlLevel, schedule: CtrlSchedule, cchain: CChainModel, L: Int, II: Int, ctx: Ctx) = this(id, level, schedule, List(cchain), L, II, ctx)

    override def toString: String = ctx.toString
    val targetBurstSize = 512
    // Control overhead
    val seqSync = 1 // cycles from end of one child to start of next
    val metaSync = 1 // cycles from end of one child to start of next
    val seqAdvance = 2 // cycles from end of last child to start of first
    val dpMask = 1 // cycles that datapath is enabled but masked by done signal
    val startup = 2
    val shutdown = 1
    val baselineDRAMLoadDelay = 170 // Cycles between single dram cmd and its response, with no competitors
    val baselineDRAMStoreDelay = 150 // Cycles between single dram cmd and its response, with no competitors
    val storeFudge = 50 // Penalty for dram store when children are run sequentially
    val congestionPenalty = 5 // Interference due to conflicting DRAM accesses
    def transfersBelow(exempt: List[ControllerModel]): Competitors = { // Count number of transfer nodes below self
      if (this.schedule == DenseLoad) Competitors.DenseLoad
      else if (this.schedule == DenseStore) Competitors.DenseStore
      else if (this.schedule == GatedDenseStore) Competitors.GatedDenseStore
      else {
        this.children.filterNot{x => exempt.map(_.ctx.id).contains(x.ctx.id)}.map(_.transfersBelow(exempt)).foldLeft(Competitors.empty){_+_} * this.cchain.head.unroll 
      }
    }
    def competitors(c: Competitors = Competitors.empty, exempt: List[ControllerModel] = List()): Competitors = { // Count number of other transfer nodes trigger simultaneously with this self
      // Number of conflicting DRAM accesses
      if (!parent.isDefined) c
      else {
        val contribution = (this.transfersBelow(exempt) + c) * parent.get.cchain.head.unroll
        val newExempt = exempt ++ List(this) ++ {if (parent.get.schedule == Sequenced) parent.get.children else List()}
        parent.get.competitors(contribution, newExempt)
      }
    }

    def congestionModel(competitors: Competitors): Int = {
      val numel = cchain.last.N
      val res = CongestionModel.evaluate(CongestionModel.RawFeatureVec(loads = competitors.loads,
                                                             stores = competitors.stores,
                                                             gateds = competitors.gateds,
                                                             outerIters = upperCChainIters,
                                                             innerIters = numel,
                                                             bitsPerCycle = this.bitsPerCycle), this.schedule)
      Console.println(s"congestion of ${competitors.loads}, ${competitors.stores}, ${competitors.gateds}, ${upperCChainIters}, ${numel}, ${this.bitsPerCycle} = $res")
      // CongestionModel.evaluate(CongestionModel.RawFeatureVec(loads = 4.8007, stores = 9, gateds = 2, outerIters = 2, innerIters = 224), this.schedule)

      // // Linear for the first 4 competitors
      // val linear1 = if (competitors < 2) competitors * congestionPenalty else 4 * congestionPenalty
      // // Exponential for the next 8 competitors
      // val exp = if (competitors < 2) 0 else if (competitors < 12) scala.math.pow(2.3, competitors - 2).toInt else scala.math.pow(2.3, 8).toInt
      // // Linear for the rest
      // val linear2 = if (competitors < 12) 0 else (competitors - 12) * congestionPenalty
      // linear1 + exp + linear2
      res
    }

    // Result fields
    var num_cycles = 1
    var num_iters = 1
    def iters_per_parent = this.num_iters / {if (parent.isDefined) parent.get.num_iters else 1}

    // Structure fields
    def depth = this.ancestors.size
    var parent: Option[ControllerModel] = None
    val children = scala.collection.mutable.ListBuffer[ControllerModel]()
    def registerChild(child: ControllerModel): Unit = {
      child.parent = Some(this)
      children += child
    }

    /** Extract num iters from cchain, or else 1 */
    def cchainIters: Int = if (cchain.size >= 1) cchain.head.N else 1

    /** Extract num iters from cchains excluding last level, or else 1 */
    def upperCChainIters: Int = if (cchain.size == 2) cchain.head.N else 1

    /** Extract max child or else 1 */
    def maxChild: Int = if (children.size >= 1) children.map(_.cycsPerParent).max else 1

    /** Extract sum of all children or else 1 */
    def sumChildren: Int = if (children.size >= 1) children.map(_.cycsPerParent).sum else 1

    /** Get ancestors of current node */
    def ancestors: Seq[ControllerModel] = {
      if (parent.isDefined) Seq(parent.get) ++ parent.get.ancestors
      else Seq()
    }
    /** Define equations for computing runtime */
    def cycsPerParent: Int = level match {
      case OuterControl => schedule match {
        case Sequenced       => 
          if (cchain.size <= 1) startup + shutdown + sumChildren * cchainIters + seqSync * children.size * cchainIters + cchainIters * seqAdvance
          else startup + shutdown + (sumChildren + cchain.last.N) * cchain.head.N + seqSync * children.size * cchain.head.N + cchain.head.N * seqAdvance
        case Pipelined if (tuneParams.contains(id) && tuneParams(id).asInstanceOf[String] == "false")      => 
          if (cchain.size <= 1) startup + shutdown + sumChildren * cchainIters + seqSync * children.size * cchainIters + cchainIters * seqAdvance
          else startup + shutdown + (sumChildren + cchain.last.N) * cchain.head.N + seqSync * children.size * cchain.head.N + cchain.head.N * seqAdvance
        case Pipelined      => 
          if (cchain.size <= 1) startup + shutdown + maxChild * (cchainIters - 1) + children.map(_.cycsPerParent).sum + metaSync * cchainIters * children.size
          else startup + shutdown + (maxChild max cchain.last.N) * (cchain.head.N - 1) + children.map(_.cycsPerParent).sum + metaSync * cchain.head.N * children.size
        case ForkJoin        => startup + shutdown + maxChild * cchainIters + metaSync
        case Streaming       => 
          if (cchain.size <= 1) startup + shutdown + maxChild * cchainIters + metaSync 
          else startup + shutdown + (maxChild max cchain.last.N) * cchain.head.N + metaSync 
        case Fork            => 
          val dutyCycles = children.dropRight(1).zipWithIndex.map{case (c,i) => Ask(c.hashCode, s"expected % of the time condition #$i will run (0-100)", ctx)}.map(_.lookup)
          children.map(_.cycsPerParent).zip(dutyCycles :+ (100-dutyCycles.sum)).map{case (a,b) => a * b.toDouble/100.0}.sum.toInt
        case DenseLoad            => congestionModel(competitors()) // upperCChainIters * (congestionModel(competitors()) * congestionPenalty + cchain.last.N) + baselineDRAMLoadDelay
        case DenseStore           => congestionModel(competitors()) // upperCChainIters * (congestionModel(competitors()) * congestionPenalty + cchain.last.N + startup + shutdown + metaSync) + baselineDRAMStoreDelay
        case GatedDenseStore      => congestionModel(competitors()) // upperCChainIters * (congestionModel(competitors()) * congestionPenalty + cchain.last.N + startup + shutdown + metaSync + baselineDRAMStoreDelay + storeFudge)
        case SparseLoad       => 1 // TODO
        case SparseStore      => 1 // TODO
      }
      case InnerControl => schedule match {
        case Sequenced => cchainIters*L + startup + shutdown
        case _ => (cchainIters - 1)*II + L + startup + shutdown + dpMask
      }
    }

    /** Simulate all Controllers in self's subtree and set their num_cycles and num_iters fields, using DFS */
    def execute(): Unit = {
      children.foreach{c => 
        c.num_iters = this.num_iters * this.cchainIters
        c.execute()
      }
      num_cycles = cycsPerParent * this.num_iters
    }
    /** Fetch AskMap values from given file */
    def initializeAskMap(map: scala.collection.mutable.Map[Int,Int]): Unit = {
      map.foreach{case (k,v) => askMap += (k -> v)}
    }

    /** Store AskMap */
    def storeAskMap(loc: String): Unit = {
      val writer = new PrintWriter(new File(loc))
      writer.write("package model\n")
      writer.write("object PreviousAskMap {\n")
      writer.write("  val map = scala.collection.mutable.Map[Int,Int]()\n")
      askMap.foreach{x => writer.write(s"  map += (${x._1} -> ${x._2})\n")}
      writer.write("}\n")
      writer.close()
    }


    /** Load Previous AskMap */
    def loadPreviousAskMap(map: scala.collection.mutable.Map[Int, Int]): Unit = {
      map.foreach{case (k,v) => 
        if (askMap.contains(k)) println(s"Warning: Overwriting parameter $k based on previous test (Used to be ${askMap(k)} -> now set to $v)")
        askMap += (k -> v)
      }
    }

    /** Indicates whether this is the last child of its parent or not, used for pretty printing */
    def lastChild: Boolean = if (!parent.isDefined) true else {(parent.get.children.map(_.ctx.id).indexOf(this.ctx.id) == (parent.get.children.size-1))}

    /** DFS through hierarchy and report performance results */
    def printResults(entry: Boolean = true): Unit = {
      if (entry) emit(s"Printing Runtime Model Results:")
      if (entry) emit("============================================")
      val cycles_per_iter = if (num_iters > 0) num_cycles / num_iters else num_cycles
      val leading = this.ancestors.reverse.map{x => if (x.lastChild) "   " else "  |"}.mkString("") + "  |"
      emit(f"${ctx.line}%5s: ${ctx.id}%6s $leading--+ ${cycles_per_iter} (${num_cycles} / ${num_iters}) [${iters_per_parent} iters/parent execution]")
      children.foreach(_.printResults(false))
      if (entry) emit("============================================")
    }

    /** DFS through hierarchy and print structure */
    def printStructure(entry: Boolean = true): Unit = {
      suppressWarns = true
      if (entry) emit(s"Controller Structure:")
      if (entry) emit("============================================")
      val leading = this.ancestors.reverse.map{x => if (x.lastChild) "   " else "  |"}.mkString("") + "  |"
      val competitors = if (this.schedule == DenseLoad) s" (${this.competitors()})"
                        else if (this.schedule == DenseStore) s" (${this.competitors()})"
                        else if (this.schedule == GatedDenseStore) s" (${this.competitors()})"
                        else ""
      // if (cchain.isDefined) emit(f"${cchain.ctx.line}%5s: ${cchain.ctx.id}%6s $leading----   (ctr: ${cchain.ctx.stm})")
      emit(f"${ctx.line}%5s: ${ctx.id}%6s $leading--+ ${ctx.info}" + competitors)
      children.foreach(_.printStructure(false))
      if (entry) emit("============================================")
      suppressWarns = false
    }

    def totalCycles(): Int = this.num_cycles
  }
}