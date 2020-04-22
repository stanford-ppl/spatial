package models

import java.io.File
import java.io.PrintWriter

import scala.io.Source


object Runtime {

  var fpga = false
  var interactive = true
  var retune = false
  var currentAsk = 0
  var logfilename: String = ""
  var logfile: Option[PrintWriter] = None
  var cliParams = Seq[Int]()
  var tuneParams = Map[String, Any]()
  var isFinal = false
  var suppressWarns = false

  /** Asked values mapping */
  // Mapping between Ask ids and their values
  val askMap = scala.collection.mutable.Map[Int, Int]()
  // Asked values for this execution
  val cachedAsk = scala.collection.mutable.ListBuffer[Int]()
  // Tuned values for this execution
  val cachedTune = scala.collection.mutable.ListBuffer[String]()

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
  case object MemReduce   extends CtrlSchedule // modeled as a schedule, used for plasticine only
  case object Reduce      extends CtrlSchedule // modeled as a schedule, used for plasticine only


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
    def op: String = {
      val pattern = """(Op+[^\(]+)""".r
      val out = pattern.findFirstIn(stm) match {
        case Some(s) => s
        case None => "None"
      }
      out
    }
  }
  object Ctx {
    def empty: Ctx = Ctx("??","??","","")
  }

  /** Base class for numbers used in model (parallelizations, counter starts/steps/ends, tile sizes, etc.) */
  abstract class ModelValue[K,V](id: K, whatAmI: String, ctx: Ctx){
    def lookup: V
  }

  /** Value that can be hot-swapped by compiler during DSE */
  case class Tuneable[V](id: String, val default: V, whatAmI: String) extends ModelValue[String,V](id, whatAmI, Ctx.empty) {
    def lookup: V = {
      if (!suppressWarns) {
        if (!tuneParams.contains(id) && !cachedTune.contains(id)) {println(s"[warn] Using default value $default for tuneable param $whatAmI ($id)"); cachedTune += id}
        else if (!cachedTune.contains(id)) {println(s"[warn] Using retuned value ${tuneParams(id)} for tuneable param $whatAmI ($id) (default was $default)"); cachedTune += id}
      }
      // println(s"looking up $id default $default in params $tuneParams")
      tuneParams.getOrElse(id, default).asInstanceOf[V]
    }
  }

  /** Value that is a constant from point of view of compiler and user */
  case class Locked(id: Int, val value: Int) extends ModelValue[Int,Int](id, "", Ctx.empty) {
    def lookup: Int = value
  }

  /** Value that must be set by user at command line, or dseModelArgs/finalModelArgs in noninteractive mode */
  case class Branch(id: Int, whatAmI: String, ctx: Ctx) extends AskBase(id, whatAmI, ctx, 50)
  case class Ask(id: Int, whatAmI: String, ctx: Ctx) extends AskBase(id, whatAmI, ctx, 1)
  abstract class AskBase(id: Int, whatAmI: String, ctx: Ctx, val base: Int = 1) extends ModelValue[Int,Int](id, whatAmI, ctx) {
    def lookup: Int = {
      if (cachedAsk.contains(id)) askMap(id)
      else if (!interactive && !cachedAsk.contains(id)) {
        val t = if (currentAsk < cliParams.size) {println(s"asking for param $currentAsk: ${cliParams(currentAsk)}"); cliParams(currentAsk)}
                else {println(s"[WARNING] Param $currentAsk not provided! Using value ${askMap.getOrElse(id, base)}"); askMap.getOrElse(id, base)}
        currentAsk = currentAsk + 1
        askMap += (id -> t)
        cachedAsk += id
        t
      }
      else if (!interactive && cachedAsk.contains(id)) {
        askMap(id)
      }
      else {
        val default = askMap.getOrElse(id, base)
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

  case class StreamCtrModel(id: Int, ctx:Ctx) extends AskBase(id, s"expected number of iterations inf stream will run (int)", ctx, 1)

  object CtrModel {
    def apply(start: Int, stop: Int, stride: Int, par: Int) =                                                                   new CtrModel[Int,Int,Int,Int](Locked(-1, start), Locked(-1, stop), Locked(-1, stride), Locked(-1, par))
    def apply[K4](start: Int, stop: Int, stride: Int, par: ModelValue[K4,Int]) =                                                new CtrModel[Int,Int,Int,K4](Locked(-1, start), Locked(-1, stop), Locked(-1, stride), par)
    def apply[K3](start: Int, stop: Int, stride: ModelValue[K3,Int], par: Int) =                                                new CtrModel[Int,Int,K3,Int](Locked(-1, start), Locked(-1, stop), stride, Locked(-1, par))
    def apply[K3,K4](start: Int, stop: Int, stride: ModelValue[K3,Int], par: ModelValue[K4,Int]) =                              new CtrModel[Int,Int,K3,K4](Locked(-1, start), Locked(-1, stop), stride, par)
    def apply[K2](start: Int, stop: ModelValue[K2,Int], stride: Int, par: Int) =                                                new CtrModel[Int,K2,Int,Int](Locked(-1, start), stop, Locked(-1, stride), Locked(-1, par))
    def apply[K2,K4](start: Int, stop: ModelValue[K2,Int], stride: Int, par: ModelValue[K4,Int]) =                              new CtrModel[Int,K2,Int,K4](Locked(-1, start), stop, Locked(-1, stride), par)
    def apply[K2,K3](start: Int, stop: ModelValue[K2,Int], stride: ModelValue[K3,Int], par: Int) =                              new CtrModel[Int,K2,K3,Int](Locked(-1, start), stop, stride, Locked(-1, par))
    def apply[K2,K3,K4](start: Int, stop: ModelValue[K2,Int], stride: ModelValue[K3,Int], par: ModelValue[K4,Int]) =            new CtrModel[Int,K2,K3,K4](Locked(-1, start), stop, stride, par)
    def apply[K1](start: ModelValue[K1,Int], stop: Int, stride: Int, par: Int) =                                                new CtrModel[K1,Int,Int,Int](start, Locked(-1, stop), Locked(-1, stride), Locked(-1, par))
    def apply[K1,K4](start: ModelValue[K1,Int], stop: Int, stride: Int, par: ModelValue[K4,Int]) =                              new CtrModel[K1,Int,Int,K4](start, Locked(-1, stop), Locked(-1, stride), par)
    def apply[K1,K3](start: ModelValue[K1,Int], stop: Int, stride: ModelValue[K3,Int], par: Int) =                              new CtrModel[K1,Int,K3,Int](start, Locked(-1, stop), stride, Locked(-1, par))
    def apply[K1,K3,K4](start: ModelValue[K1,Int], stop: Int, stride: ModelValue[K3,Int], par: ModelValue[K4,Int]) =            new CtrModel[K1,Int,K3,K4](start, Locked(-1, stop), stride, par)
    def apply[K1,K2](start: ModelValue[K1,Int], stop: ModelValue[K2,Int], stride: Int, par: Int) =                              new CtrModel[K1,K2,Int,Int](start, stop, Locked(-1, stride), Locked(-1, par))
    def apply[K1,K2,K4](start: ModelValue[K1,Int], stop: ModelValue[K2,Int], stride: Int, par: ModelValue[K4,Int]) =            new CtrModel[K1,K2,Int,K4](start, stop, Locked(-1, stride), par)
    def apply[K1,K2,K3](start: ModelValue[K1,Int], stop: ModelValue[K2,Int], stride: ModelValue[K3,Int], par: Int) =            new CtrModel[K1,K2,K3,Int](start, stop, stride, Locked(-1, par))    
    def apply[K1,K2,K3,K4](start: ModelValue[K1,Int], stop: ModelValue[K2,Int], stride: ModelValue[K3,Int], par: ModelValue[K4,Int]) = new CtrModel[K1,K2,K3,K4](start, stop, stride, par)    
  }
  class CtrModel[K1,K2,K3,K4](
    val start: ModelValue[K1,Int],
    val stop: ModelValue[K2,Int], 
    val stride: ModelValue[K3,Int],
    val par: ModelValue[K4,Int]
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
    def Nd: Double = {
      val realStart = start.lookup
      val realStop = stop.lookup
      val realStride = stride.lookup
      val realPar = par.lookup
      scala.math.ceil((realStop - realStart).toDouble / realStride.toDouble).toDouble / realPar.toDouble
    }
  }

  // Needed for plasticine memory dse analysis
  // Alias memory used for mem reduce
  case class AliasMemModel(val mem: MemModel) {
    def physSize: Int = mem.physSize
    def size: Int = mem.size
    def memRead(readParent: ControllerModel): Unit = {
      mem.memRead(readParent)
    }
    def memWrite(writeParent: ControllerModel): Unit = {
      mem.memWrite(writeParent)
    } 
  }

  case class MemModel(
    val id:String, 
    val dims:Seq[Int],
    val bitwidth:Int
  ) {
    var parent: Option[ControllerModel] = None
    val readers = scala.collection.mutable.ListBuffer[ControllerModel]()
    val writers = scala.collection.mutable.ListBuffer[ControllerModel]()

    def roundUp(n: Int, t: Int): Int = {
      if (n == 0) 0 else {((n + t - 1).toDouble / t.toDouble).toInt * t}
    }
    val pmuSize = 256*1024/4 // elements  //* 8 // bits
    def physSize: Int = roundUp(this.size, pmuSize) / pmuSize  
    def size: Int = {
      dims.reduceLeft(_*_) 
    }
    def memRead(readParent: ControllerModel): Unit = {
       readers += readParent
       readParent.memReads += this
    } 
    def memWrite(writeParent: ControllerModel): Unit = {
        writers += writeParent
        writeParent.memWrites += this
    }

    def writersMaxCycles: Int = if (writers.size >= 1) writers.toList.map(_.cycles_per_iter).max else 1

    override def toString: String = s"mem: $id"
  }

  case class CChainModel(
    val ctrs: Seq[CtrModel[_,_,_,_]],
    val ctx: Ctx = Ctx("","","","")
  ) {
    def N: Int = { // Num iters for lane
      if (ctrs.isEmpty) 1 
      else ctrs.map(_.N).product
    }
    def Nd: Double = { 
      if (ctrs.isEmpty) 1
      else ctrs.map(_.Nd).product
    }
    def unroll: Int = { 
      if (isFinal) 1 else ctrs.map(_.par.lookup).product
    }
    def isDefined: Boolean = !ctrs.isEmpty
  }

  class ControllerModel(
    val id: Int,
    val level: CtrlLevel,
    val schedule: Either[CtrlSchedule,Tuneable[String]],
    val cchain: List[CChainModel],
    val L: Int,
    val II: Int,
    val ctx: Ctx,
    val bitsPerCycle: Double = 32.0 // Defined for transfers
  ){
    def this(id: Int, level: CtrlLevel, schedule: Either[CtrlSchedule,Tuneable[String]], cchain: CChainModel, L: Int, II: Int, ctx: Ctx) = this(id, level, schedule, List(cchain), L, II, ctx)

    override def toString: String = ctx.toString
    val targetBurstSize = 512
    // Control overhead per Iteration
    val seqSync = 1 // cycles from end of one child to start of next
    val metaSync = 1 // cycles from end of one child to start of next
    val seqAdvance = 2 // cycles from end of last child to start of first
    val dpMask = 1 // cycles that datapath is enabled but masked by done signal
    val startup = 2
    val shutdown = 1

    // Plasticine overhead total
    val plastPipeDepth = 6                      // Plasticine pipeline depth
    val ctrlStartup = 2
    val plastSeq = 1
    val plastIdealRoute = 3                     // Number of cycles to communicate b/w neighboring units using ideal routing network
    val plastMemReduceII = 2*plastIdealRoute + 2+ plastPipeDepth   // 3 cycles on ideal route plus pipeline depth 

    // Schedule helpers to handle tuneable nodes
    def isSeq = schedule match {
      case Left(Sequenced) => true
      case Right(x) if x.lookup == "false" => true
      case _ => false
    }

    def isReduce = schedule match {
      case Left(x) if (ctx.op == "OpReduce") => true
      case _ => false
    }

    def resolvedSchedule = schedule match {
      case Left(x) if (!fpga && ctx.op == "OpMemReduce") => MemReduce 
      case Left(x) => x
      case Right(x) if x.lookup == "false" => Sequenced
      case _ => Pipelined
    }

    def transfersBelow(exempt: List[ControllerModel]): Competitors = { // Count number of transfer nodes below self
      this.schedule match {
        case Left(DenseLoad) => Competitors.DenseLoad
        case Left(DenseStore) => Competitors.DenseStore
        case Left(GatedDenseStore) => Competitors.GatedDenseStore
        case _ => this.children.filterNot{x => exempt.map(_.ctx.id).contains(x.ctx.id)}.map(_.transfersBelow(exempt)).foldLeft(Competitors.empty){_+_} * this.cchain.head.unroll 
      }
    }
    def competitors(c: Competitors = Competitors.empty, exempt: List[ControllerModel] = List()): Competitors = { // Count number of other transfer nodes trigger simultaneously with this self
      // Number of conflicting DRAM accesses
      if (!parent.isDefined) c
      else {
        val contribution = (this.transfersBelow(exempt) + c) * parent.get.cchain.head.unroll
        val newExempt = exempt ++ List(this) ++ {if (parent.get.isSeq) parent.get.children else List()}
        parent.get.competitors(contribution, newExempt)
      }
    }

    private def burstAlign(numel: Double, bitsPerCycle: Int): Double = {
      val burstSize = 512 // bits
      val bitsPerCommand = numel * bitsPerCycle
      val x = if (bitsPerCommand % burstSize == 0) numel
              else ((burstSize - (bitsPerCommand % burstSize)) / bitsPerCycle).toInt + numel
      x
    }
    def congestionModel(competitors: Competitors): Int = {
      emit(s"num el: ${cchain.last.N}")
      val numel = burstAlign(cchain.last.N.toDouble, this.bitsPerCycle.toInt)
      // // Lattice regression
      // val res = CongestionModel.evaluate(CongestionModel.RawFeatureVec(loads = competitors.loads,
      //                                                        stores = competitors.stores,
      //                                                        gateds = competitors.gateds,
      //                                                        outerIters = upperCChainIters,
      //                                                        innerIters = numel,
      //                                                        bitsPerCycle = this.bitsPerCycle), this.resolvedSchedule)
      // res

      // curve_fit
      def params(x: Seq[Double]): (Double, Double, Double, Double, Double, Double) = (x(0), x(1), x(2), x(3), x(4), x(5))

      def fitFunc4(x: Seq[Double], congestion: Double, stallPenalty: Double, idle: Double, startup: Double, parFactor: Double, a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double, h: Double, i: Double, j: Double): Double = {
        val (loads, stores, gateds, outerIters, innerIters, bitsPerCycle) = params(x)
        val countersContribution = outerIters * (innerIters + idle)
        val congestionContribution = (loads*a + stores*b + gateds*c)*congestion
        val parallelizationScale = bitsPerCycle * parFactor
        (countersContribution * stallPenalty * (congestionContribution + countersContribution / bitsPerCycle * j) + startup) * parallelizationScale
      }
      val p = ModelData.curve_fit(this.resolvedSchedule.toString).map(_.toDouble)
      val r = 170 max fitFunc4(Seq(competitors.loads, competitors.stores, competitors.gateds, upperCChainIters, numel, this.bitsPerCycle).map(_.toDouble), p(0),p(1),p(2),p(3),p(4),p(5),p(6),p(7),p(8),p(9),p(10),p(11),p(12),p(13),p(14)).toInt
      Console.println(s"infer on ${Seq(competitors.loads, competitors.stores, competitors.gateds, upperCChainIters, numel, this.bitsPerCycle).map(_.toDouble)} = $r")
      r
    }

    def congestionModelPlasticine(competitors: Competitors, store:Boolean): Int = {
      val numel = burstAlign(cchain.last.N.toDouble, this.bitsPerCycle.toInt)
      val numelD = cchain.last.Nd

      def params(x: Seq[Double]): (Double, Double, Double, Double, Double, Double) = (x(0), x(1), x(2), x(3), x(4), x(5))

      def fitFunc(x: Seq[Double]): Double = {
        val maxBurstSize = 800 // bits per cycle. From DDR4 peak bw 100 GB/s
        val (loads, stores, gateds, outerIters, innerIters, bitsPerCycle) = params(x)
        val comp = if (store) stores + gateds else loads
        outerIters * innerIters * ((comp * bitsPerCycle / maxBurstSize) max 1) 
      }

      val r = 1 max (fitFunc(Seq(competitors.loads, competitors.stores, competitors.gateds, upperCChainIters, numel, this.bitsPerCycle).map(_.toDouble)).toInt + 1)
 
      emit(s"Store: ${store}, infer on ${Seq(competitors.loads, competitors.stores, competitors.gateds, upperCChainIters, numel, this.bitsPerCycle).map(_.toDouble)} = $r")
      r
    }


    // Result fields
    var num_cycles = 1
    var num_iters = 1
    var num_iters_d = 1.0
    var cycles_per_iter = 1

    // TODO: Add in latency
    var plastLatency = 0

    var forkPercent = List(1) 

    def iters_per_parent = this.num_iters / (if (parent.isDefined) parent.get.num_iters else 1)

    // Structure fields
    def depth = this.ancestors.size
    var parent: Option[ControllerModel] = None
    val children = scala.collection.mutable.ListBuffer[ControllerModel]()
    val memChildren = scala.collection.mutable.ListBuffer[MemModel]()
    val memReads = scala.collection.mutable.ListBuffer[MemModel]()
    val memWrites = scala.collection.mutable.ListBuffer[MemModel]()

    def isLeaf: Boolean = !(children.size >= 1)
 
    def childMemReads: List[MemModel] = if (children.size >= 1) children.map(_.childMemReads).foldRight(memReads.toList)(_++_) else memReads.toList
    def childMemWrites: List[MemModel] = if (children.size >= 1) children.map(_.childMemWrites).foldRight(memWrites.toList)(_++_) else memWrites.toList

    def memReadChain: List[(MemModel, List[ControllerModel])] = if (children.size >= 1) 
        children.map(_.memReadChain.map{case (m, l) => (m, this :: l)}).foldRight(memReads.toList.map{case s => (s, List(this))})(_++_) else
        memReads.toList.map{case s => (s, List(this))}

/*
    def memReadChainD: List[(MemModel, Int)] = if (children.size >= 1) children.map(_.memReadChain.map{case (m, l) => (m, l.size)}).foldRight(List(): List[(MemModel, Int)])(_++_) else List() : List[(MemModel, Int)]
    def memWriteChainD: List[(MemModel, Int)] = if (children.size >= 1) children.map(_.memWriteChain.map{case (m, l) => (m, l.size)}).foldRight(List(): List[(MemModel, Int)])(_++_) else List(): List[(MemModel, Int)]

    def hasLoopII: Boolean = { 
      for (e1 <- memReadChainD) {
        for (e2 <- memWriteChainD) {
          if (e1._1 == e2._1 && e1._2 != e2._2)
            true
        }
      }
      false 
    }    
*/    
    def plastLoopII: Int = {
      var out: Int = 1
      for (mRead <- memReadChain) {
        for (mWrite <- memWriteChain) {
          if (mRead._1 == mWrite._1 && mRead._2.size != mWrite._2.size)
            out = mRead._2.map(_.cycles_per_iter).sum + mWrite._2.map(_.cycles_per_iter).sum
            //out = mRead._2.drop(1).map(_.cycles_per_iter).sum + mWrite._2.drop(1).map(_.cycles_per_iter).sum
        }
      }
      out
    }

    def memWriteChain: List[(MemModel, List[ControllerModel])] = if (children.size >= 1) 
      children.map(_.memWriteChain.map{case (m, l) => (m, this :: l)}).foldRight(memWrites.toList.map{case s => (s, List(this))})(_++_) else
      memWrites.toList.map{case s => (s, List(this))}
    //def plastMemLoop:  
 
    def registerChild(child: ControllerModel): Unit = {
      child.parent = Some(this)
      children += child
    }

    def registerMemChild(memChild: MemModel): Unit = {
      memChild.parent = Some(this)
      memChildren += memChild
    }

    def registerMemRead(memChild: AliasMemModel): Unit = {
      memChild.mem.readers += this
      memReads += memChild.mem
    }

    def registerMemWrite(memChild: AliasMemModel): Unit = {
      memChild.mem.writers += this
      memWrites += memChild.mem
    }

    def registerMemRead(memChild: MemModel): Unit = {
      memChild.readers += this
      memReads += memChild
    }

    def registerMemWrite(memChild: MemModel): Unit = {
      memChild.writers += this
      memWrites += memChild
    }

    def registerForkPct(fp: List[Int]): Unit = {
        forkPercent = fp
    }

    /** Extract num iters from cchain, or else 1 */
    def cchainIters: Int = if (cchain.size >= 1) cchain.head.N else 1
    def cchainItersD: Double = if (cchain.size >= 1) cchain.head.Nd else 1.0

    /** Extract num iters from cchains excluding last level, or else 1 */
    def upperCChainIters: Int = if (cchain.size == 2) cchain.head.N else 1
    def upperCChainItersD: Double = if (cchain.size == 2) cchain.head.Nd else 1.0

    /** Extract max child or else 1 */
    def maxChild: Int = if (children.size >= 1) children.map(_.cycles_per_iter).max else 1

    /** Extract sum of all children or else 1 */
    def sumChildren: Int = if (children.size >= 1) children.map(_.cycles_per_iter).sum else 1

    def maxAdjacentChildren: Int = children.map(_.cycles_per_iter).sliding(2).map(_.sum).toList.max

    def memSplitAdjustment: Int  = memReads.map(_.physSize).product * memWrites.map(_.physSize).product

    def plastMemII: Int = if (childMemReads.size >= 1) childMemReads.map(_.writersMaxCycles).max else 1

    def plastMaxLatency: Int = maxChild max plastMemII
    def plastSumLatency: Int = sumChildren max plastMemII

    /** Get ancestors of current node */
    def ancestors: Seq[ControllerModel] = {
      if (parent.isDefined) Seq(parent.get) ++ parent.get.ancestors
      else Seq()
    }
    /** Define equations for computing runtime */
    def cycsPerParent: Int = level match {
      case OuterControl => resolvedSchedule match {
        case Sequenced       => 
          if (fpga) {
            if (cchain.size <= 1) startup + shutdown + sumChildren * cchainIters + seqSync * children.size * cchainIters + cchainIters * seqAdvance
            else startup + shutdown + (sumChildren + cchain.last.N) * cchain.head.N + seqSync * children.size * cchain.head.N + cchain.head.N * seqAdvance
          } else {
            emit(s"ctx: ${ctx.id}, sumChild: ${sumChildren}, maxChild: ${maxChild}, memAdjustment: ${memSplitAdjustment}, maxAdjChild: ${maxAdjacentChildren}, cchainIters: ${cchainIters}, lastN: ${cchain.last.N}, headN ${cchain.head.N}, childSize: ${children.size}")
            emit(s"ctx: ${ctx.id}, children.cycs_per_iter: ${children.map(_.cycles_per_iter)}")
            emit(s"ctx: ${ctx.id}, childMemReads (writers): ${childMemReads.map(_.writers)}, childMemReads: ${childMemReads}, plastMemII: $plastMemII")
            emit(s"plastLoopII: $plastLoopII")
            //emit(s"memReadChain: $memReadChain")
            //emit(s"memWritechain: $memWriteChain")
            val interval = (sumChildren min maxAdjacentChildren) max plastLoopII
            ((interval) * cchainIters) //+ seqSync * children.size*cchain.head.N
          }
        case Pipelined      => 
          if (fpga) {
            if (cchain.size <= 1) startup + shutdown + maxChild * (cchainIters - 1) + children.map(_.cycsPerParent).sum + metaSync * cchainIters * children.size
            else startup + shutdown + (maxChild max cchain.last.N) * (cchain.head.N - 1) + children.map(_.cycsPerParent).sum + metaSync * cchain.head.N * children.size
          } else {
            emit(s"ctx: ${ctx.id}, maxChild: ${maxChild}, cchainIters: ${cchainIters}, lastN: ${cchain.last.N}, headN ${cchain.head.N}")
            emit(s"ctx: ${ctx.id}, childMemReads (writers): ${childMemReads.map(_.writers)}, memReadChain: $memReadChain, plastMemII: $plastMemII")
            emit(s"plastLoopII: $plastLoopII")
            //emit(s"memReadChain: $memReadChain")
            //emit(s"memWritechain: $memWriteChain")
            val interval = maxChild max plastLoopII
            if (cchain.size <= 1) interval * (cchainIters)
            else (interval max cchain.last.N) * (cchain.head.N) 
          }
        case MemReduce       =>
          if (fpga) {
            if (cchain.size <= 1) startup + shutdown + maxChild * (cchainIters - 1) + children.map(_.cycsPerParent).sum + metaSync * cchainIters * children.size
            else startup + shutdown + (maxChild max cchain.last.N) * (cchain.head.N - 1) + children.map(_.cycsPerParent).sum + metaSync * cchain.head.N * children.size
          } else {
//            if (cchain.size <= 1) maxChild * (cchainIters)
//            else (maxChild max cchain.last.N) * (cchain.head.N) 
            emit(s"ctx: ${ctx.id}, sumChild: ${sumChildren}, maxChild: ${maxChild}, cchainIters: ${cchainIters}, lastN: ${cchain.last.N}, headN ${cchain.head.N}")
            ((maxChild max cchain.last.N) + 24 ) * cchainIters//plastMemReduceII) * cchainIters
          }
        case ForkJoin        => 
          if (fpga) startup + shutdown + maxChild * cchainIters + metaSync
          else maxChild * cchainIters
        case Streaming       => 
          if (fpga) {
            emit(s"ctx: ${ctx.id}, sumChild: ${sumChildren}, maxChild: ${maxChild}, cchainSize: ${cchain.size}, cchainIters: ${cchainIters}, lastN: ${cchain.last.N}, headN ${cchain.head.N}")
            if (cchain.size <= 1) startup + shutdown + maxChild * cchainIters + metaSync 
            else startup + shutdown + (maxChild max cchain.last.N) * cchain.head.N + metaSync 
          } else {
            emit(s"ctx: ${ctx.id}, sumChild: ${sumChildren}, maxChild: ${maxChild}, cchainSize: ${cchain.size}, cchainIters: ${cchainIters}, lastN: ${cchain.last.N}, headN ${cchain.head.N}")
            emit(s"ctx: ${ctx.id}, children.cycs_per_iter: ${children.map(_.cycles_per_iter)}")
            emit(s"ctx: ${ctx.id}, childMemReads (writers): ${childMemReads.map(_.writers)}, childMemReads: ${childMemReads}, memReadChain: $memReadChain, plastMemII: $plastMemII")
            if (cchain.size <= 1) startup + shutdown + maxChild * cchainIters + metaSync 
            else startup + shutdown + (maxChild max cchain.last.N) * cchain.head.N + metaSync 
          }
        case Fork            => 
//          val dutyCycles = children.dropRight(1).zipWithIndex.map{case (c,i) => Branch(c.hashCode, s"expected % of the time condition #$i will run (0-100)", ctx)}.map(_.lookup)
//          emit(s"${children.map(_.cycsPerParent).zip(dutyCycles :+ (100-dutyCycles.sum))}")
//          children.map(_.cycsPerParent).zip(dutyCycles :+ (100-dutyCycles.sum)).map{case (a,b) => a * b.toDouble/100.0}.sum.toInt
//          emit(s"${children.map(_.cycsPerParent).zip(forkPercent :+ (100-forkPercent.sum))}")
          children.map(_.cycsPerParent).zip(forkPercent :+ (100-forkPercent.sum)).map{case (a,b) => a * b.toDouble/100.0}.sum.toInt
        case DenseLoad            => if (fpga) congestionModel(competitors()) else congestionModelPlasticine(competitors(), false)
        case DenseStore           => if (fpga) congestionModel(competitors()) else congestionModelPlasticine(competitors(), true)
        case GatedDenseStore      => if (fpga) congestionModel(competitors()) else congestionModelPlasticine(competitors(), true)
        case SparseLoad       => 1 // TODO
        case SparseStore      => 1 // TODO
      }
      case InnerControl => resolvedSchedule match {
        case Sequenced => 
          if (fpga) cchainIters*L + startup + shutdown
          else {
            if (isReduce) (cchainIters*L + plastSeq)
            else {
              emit(s"ctx: ${ctx.id}, memAdj: $memSplitAdjustment, cchainIters: ${cchainIters}, L: $L") 
              (cchainIters*L + plastSeq) 
            }
          }
        case _ => 
          if (fpga) (cchainIters - 1)*II + L + startup + shutdown + dpMask
          else cchainIters*II 
      }
    }

    /** Simulate all Controllers in self's subtree and set their num_cycles and num_iters fields, using DFS */
    def execute(): Unit = {
      children.foreach{c => 
        c.num_iters = this.num_iters * this.cchainIters
        c.num_iters_d = this.num_iters_d * this.cchainItersD
        c.execute()
      }
//      num_cycles = cycsPerParent * this.num_iters
      num_cycles = if (fpga) {
        cycsPerParent * this.num_iters
      } else { 
        level match {
          case OuterControl => resolvedSchedule match {
            case DenseLoad => (cycsPerParent.toDouble * this.num_iters_d).toInt
            case DenseStore => (cycsPerParent.toDouble * this.num_iters_d).toInt
            case GatedDenseStore => (cycsPerParent.toDouble * this.num_iters_d).toInt
            case _ => cycsPerParent * this.num_iters
          }
          case InnerControl => cycsPerParent * this.num_iters
        }
      }
      // FIXME: This might not be cofrrect. 
      if (!fpga && this.num_iters <= 1) num_cycles = plastPipeDepth max num_cycles
      cycles_per_iter = if (num_iters > 0) num_cycles / num_iters else num_cycles
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
      val competitors = if (this.schedule == Left(DenseLoad)) s" (${this.competitors()})"
                        else if (this.schedule == Left(DenseStore)) s" (${this.competitors()})"
                        else if (this.schedule == Left(GatedDenseStore)) s" (${this.competitors()})"
                        else ""
      // if (cchain.isDefined) emit(f"${cchain.ctx.line}%5s: ${cchain.ctx.id}%6s $leading----   (ctr: ${cchain.ctx.stm})")
      emit(f"${ctx.line}%5s: ${ctx.id}%6s $leading--+ ${ctx.info} " + competitors)
      children.foreach(_.printStructure(false))
      if (entry) emit("============================================")
      suppressWarns = false
    }

    def totalCycles(): Int = this.num_cycles
  }
}
