package models

import java.io.File
import java.io.PrintWriter

import scala.io.Source


object Runtime {

  /** Asked values mapping */
  // Mapping between Ask ids and their values
  val askMap = scala.collection.mutable.Map[Int, Int]()
  // Asked values for this execution
  val cached = scala.collection.mutable.ListBuffer[Int]()

  /** Control node schedule */
  sealed abstract class CtrlSchedule
  case object Sequenced  extends CtrlSchedule
  case object Pipelined  extends CtrlSchedule
  case object Streaming  extends CtrlSchedule
  case object ForkJoin   extends CtrlSchedule
  case object Fork       extends CtrlSchedule
  case object DenseStore extends CtrlSchedule // modeled as a schedule
  case object DenseLoad  extends CtrlSchedule // modeled as a schedule


  /** Control node level. */
  sealed abstract class CtrlLevel
  case object InnerControl extends CtrlLevel
  case object OuterControl extends CtrlLevel

  /** Info about node */ 
  case class Ctx(
    val id: String, // Sym name (i.e. x####)
    val line: String, // Line number of item
    val info: String,  // Code for this node (i.e. "Foreach(N){i => " )
    val stm: String // IR node from spatial
  ){
    override def toString: String = s"line $line: $id"
  }

  case class Ask(val id: Int, val whatAmI: String, ctx: Ctx) {
    def lookup: Int = {
      if (cached.contains(id)) askMap(id)
      else {
        val default = askMap.getOrElse(id, 1)
        print(s"Value for $whatAmI (${ctx.toString}) [default: $default] : ")
        val t = scala.io.StdIn.readLine()
        val x = if (t != "") t.toInt else default
        println("")
        askMap += (id -> x)
        cached += id
        x
      }
    }
  }

  case class CtrModel(
    val start: Either[Int, Ask],
    val stop: Either[Int, Ask], 
    val stride: Either[Int, Ask],
    val par: Int
  ) {
    def N: Int = {
      val realStart = start match {case Left(x) => x; case Right(x) => x.lookup}
      val realStop = stop match {case Left(x) => x; case Right(x) => x.lookup}
      val realStride = stride match {case Left(x) => x; case Right(x) => x.lookup}
      scala.math.ceil((realStop - realStart).toDouble / (realStride * par).toDouble).toInt
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
      ctrs.map(_.par).product
    }
    def isDefined: Boolean = !ctrs.isEmpty
  }

  class ControllerModel(
    val level: CtrlLevel,
    val schedule: CtrlSchedule,
    val cchain: CChainModel,
    val L: Int,
    val II: Int,
    val ctx: Ctx
  ){
    // Control overhead
    val seqSync = 1 // cycles from end of one child to start of next
    val metaSync = 1 // cycles from end of one child to start of next
    val seqAdvance = 2 // cycles from end of last child to start of first
    val dpMask = 1 // cycles that datapath is enabled but masked by done signal
    val startup = 2
    val shutdown = 1
    val baselineDRAMDelay = 200 // Cycles between single dram cmd and its response, with no competitors
    val congestionPenalty = 10 // Interference due to conflicting DRAM accesses
    def transfersBelow: Int = { // Count number of transfer nodes below self
      if (this.schedule == DenseLoad || this.schedule == DenseStore) 1
      else this.cchain.unroll * this.children.map(_.transfersBelow).sum
    }
    def competitors(c: Int): Int = { // Count number of other transfer nodes trigger simultaneously with this self
      // Number of conflicting DRAM accesses
      if (!parent.isDefined) c
      else {
        parent.get.schedule match {
          case Sequenced => parent.get.cchain.unroll * c
          case _ => parent.get.cchain.unroll * (parent.get.children.filter(_.ctx.id != this.ctx.id).map{x => x.transfersBelow}.sum + c)
        }
      }
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

    /** Get ancestors of current node */
    def ancestors: Seq[ControllerModel] = {
      if (parent.isDefined) Seq(parent.get) ++ parent.get.ancestors
      else Seq()
    }
    /** Define equations for computing runtime */
    def cycsPerParent: Int = level match {
      case OuterControl => schedule match {
        case Sequenced       => startup + shutdown + children.map(_.cycsPerParent).sum * cchain.N + seqSync * children.map(_.cycsPerParent).size * cchain.N + cchain.N * seqAdvance
        case Pipelined       => startup + shutdown + children.map(_.cycsPerParent).max * (cchain.N - 1) + children.map(_.cycsPerParent).sum + metaSync * cchain.N * children.map(_.cycsPerParent).size
        case ForkJoin        => startup + shutdown + children.map(_.cycsPerParent).max * cchain.N + metaSync
        case Streaming       => startup + shutdown + children.map(_.cycsPerParent).max * cchain.N + metaSync // TODO: lookup and use baselineDRAMDelay
        case Fork            => startup + shutdown + children.map(_.cycsPerParent).max * cchain.N + metaSync // TODO: lookup and use duty cycle
        case DenseLoad       => cchain.ctrs.dropRight(1).map(_.N).product * (competitors(1) * congestionPenalty + cchain.ctrs.last.N) + baselineDRAMDelay
        case DenseStore      => cchain.ctrs.dropRight(1).map(_.N).product * (competitors(1) * congestionPenalty + cchain.ctrs.last.N) + baselineDRAMDelay
      }
      case InnerControl => schedule match {
        case Sequenced => cchain.N*L + startup + shutdown
        case _ => (cchain.N - 1)*II + L + startup + shutdown + dpMask
      }
    }

    /** Simulate all Controllers in self's subtree and set their num_cycles and num_iters fields, using DFS */
    def execute(): Unit = {
      children.foreach{c => 
        c.num_iters = this.num_iters * this.cchain.N
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
      if (entry) println(s"Printing Runtime Model Results:")
      if (entry) println("============================================")
      val cycles_per_iter = if (num_iters > 0) num_cycles / num_iters else num_cycles
      val leading = this.ancestors.reverse.map{x => if (x.lastChild) "   " else "  |"}.mkString("") + "  |"
      println(f"${ctx.line}%5s: ${ctx.id}%6s $leading--+ ${cycles_per_iter} (${num_cycles} / ${num_iters}) [${iters_per_parent} iters/parent execution]")
      children.foreach(_.printResults(false))
      if (entry) println("============================================")
    }

    /** DFS through hierarchy and print structure */
    def printStructure(entry: Boolean = true): Unit = {
      if (entry) println(s"Controller Structure:")
      if (entry) println("============================================")
      val leading = this.ancestors.reverse.map{x => if (x.lastChild) "   " else "  |"}.mkString("") + "  |"
      val competitors = if (this.schedule == DenseLoad || this.schedule == DenseStore) s" (${this.competitors(1)} competitors)" else ""
      // if (cchain.isDefined) println(f"${cchain.ctx.line}%5s: ${cchain.ctx.id}%6s $leading----   (ctr: ${cchain.ctx.stm})")
      println(f"${ctx.line}%5s: ${ctx.id}%6s $leading--+ ${ctx.info}" + competitors)
      children.foreach(_.printStructure(false))
      if (entry) println("============================================")
    }

    def totalCycles(): Int = this.num_cycles
  }
}