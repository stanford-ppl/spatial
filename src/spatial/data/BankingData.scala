package spatial.data

import forge.tags._
import utils.implicits.collections._
import argon._

import spatial.lang.LocalMem
import spatial.internal.spatialConfig
import spatial.targets.MemoryResource
import spatial.util._

/** Abstract class for any banking strategy. */
sealed abstract class Banking {
  def nBanks: Int
  def stride: Int
  def dims: Seq[Int]
  @api def bankSelect[I:IntLike](addr: Seq[I]): I
}

/** Banking address function (alpha*A / B) mod N. */
case class ModBanking(N: Int, B: Int, alpha: Seq[Int], dims: Seq[Int]) extends Banking {
  override def nBanks: Int = N
  override def stride: Int = B

  @api def bankSelect[I:IntLike](addr: Seq[I]): I = {
    import spatial.util.IntLike._
    (alpha.zip(addr).map{case (a,i) => a*i }.sumTree / B) % N
  }
  override def toString: String = {
    val name = if (B == 1) "Cyclic" else "Block Cyclic"
    s"Dims {${dims.mkString(",")}}: $name: N=$N, B=$B, alpha=<${alpha.mkString(",")}>"
  }
}
object ModBanking {
  def Unit(rank: Int) = ModBanking(1, 1, Seq.fill(rank)(1), Seq.tabulate(rank){i => i})
}


/** Helper structure for holding metadata for buffer ports
  * The mux port is the time multiplexed slot the associated access has in reference to all other accesses
  * on the same port occurring at the same time.
  *
  * The buffer port is the port at which the associated access should be connected for N-buffers.
  * If the buffer is depth 1, this is always Some(0).
  * If the access occurs outside of the metapipeline that uses this buffer, the port will be None.
  */
case class Port(muxPort: Int, bufferPort: Option[Int])


/** Used during memory analysis to track intermediate results. */
case class Instance(
  reads:    Set[Set[AccessMatrix]], // All reads within this group
  writes:   Set[Set[AccessMatrix]], // All writes within this group
  ctrls:    Set[Ctrl],              // Set of controllers these accesses are in
  metapipe: Option[Ctrl],           // Controller if at least some accesses require n-buffering
  banking:  Seq[Banking],           // Banking information
  depth:    Int,                    // Depth of n-buffer
  cost:     Int,                    // Cost estimate of this configuration
  ports:    Map[AccessMatrix,Port], // Buffer ports
  accType:  AccumType               // Type of accumulator for instance
) {
  def toMemory: Memory = Memory(banking, depth, accType)

  override def toString: String = {
    import scala.math.Ordering.Implicits._

    def prtStr(port: Option[Int], grps: Set[Set[AccessMatrix]], tp: String): Iterator[String] = {
      grps.iterator.flatten
          .filter{a => ports(a).bufferPort == port }    // All accesses on this port
          .toSeq.groupBy(_.access)                      // Group by access (symbol)
          .iterator.flatMap(_._2.sortBy(_.unroll))      // Lexicographic sort by unroll ID
          .map{a => s"  ${port.getOrElse("M")}: (mux:${ports(a).muxPort}) [$tp] ${stm(a.access)} {${a.unroll.mkString(", ")}}" }
    }

    val ps = (0 until depth).map{Some(_)} :+ None

    s"""
       |accumType:  $accType
       |Depth:      $depth
       |Banking:    $banking
       |Controller: ${metapipe.map(_.toString).getOrElse("---")}
       |Buffer Ports:""".stripMargin + "\n" +
    ps.flatMap{port => prtStr(port,writes,"WR") ++ prtStr(port,reads,"RD") }.mkString("\n")
  }

}
object Instance {
  def Unit(rank: Int) = Instance(Set.empty,Set.empty,Set.empty,None,Seq(ModBanking.Unit(rank)),1,0,Map.empty,AccumType.None)
}


/**
  * Abbreviated version of MemoryInstance for use outside memory analysis
  */
case class Memory(
  banking: Seq[Banking],  // Banking information
  depth:   Int,           // Buffer depth
  accType: AccumType      // Flags whether this instance is an accumulator
) {
  var resourceType: Option[MemoryResource] = None
  @stateful def resource: MemoryResource = resourceType.getOrElse(spatialConfig.target.defaultResource)

  def nBanks: Seq[Int] = banking.map(_.nBanks)
  def totalBanks: Int = banking.map(_.nBanks).product
  def bankDepth(dims: Seq[Int]): Int = {
    banking.map{bank =>
      val size = bank.dims.map{i => dims(i) }.product
      Math.ceil(size.toDouble / bank.nBanks)    // Assumes evenly divided
    }.product.toInt
  }

  @api def bankSelects[T:IntLike](addr: Seq[T]): Seq[T] = banking.map(_.bankSelect(addr))

  @api def bankOffset[T:IntLike](mem: Sym[_], addr: Seq[T]): T = {
    import spatial.util.IntLike._
    val w = dimsOf(mem).map(_.toInt)
    val D = rankOf(mem)
    val n = banking.map(_.nBanks).product
    if (banking.lengthIs(1)) {
      val b = banking.head.stride

      (0 until D).map{t =>
        val xt = addr(t)
        if (t < D - 1) { xt * (w.slice(t+1,D-1).product * math.ceil(w(D-1).toDouble / (n*b)).toInt * b) }
        else           { (xt / (n*b)) * b + xt % b }
      }.sumTree
    }
    else if (banking.lengthIs(D)) {
      val b = banking.map(_.stride)
      val n = banking.map(_.nBanks)
      val dims = (0 until D).map{t => (t+1 until D).map{k => math.ceil(w(k)/n(k)).toInt }.product }

      (0 until D).map{t =>
        val xt = addr(t)
        ( ( xt/(b(t)*n(t)) )*b(t) + xt%b(t) ) * dims(t)
      }.sumTree
    }
    else {
      // TODO: Bank address for mixed dimension groups
      throw new Exception("Bank address calculation for arbitrary dim groupings unknown")
    }
  }
}
object Memory {
  def unit(rank: Int): Memory = Memory(Seq(ModBanking.Unit(rank)), 1, AccumType.None)
}


/** Physical duplicates metadata for a single logical memory.
  * Multiple duplicates may be necessary to help support random access read bandwidth.
  * After IR unrolling, each memory node should have exactly one duplicate.
  *
  * Pre-unrolling:
  * Option:  sym.getDuplicates
  * Getter:  sym.duplicates
  * Setter:  sym.duplicates = (Seq[Memory])
  * Default: undefined
  *
  * Post-unrolling:
  * Option:  sym.getInstance
  * Getter:  sym.instance
  * Setter:  sym.instance = (Memory)
  * Default: undefined
  */
case class Duplicates(d: Seq[Memory]) extends StableData[Duplicates]


/** Map of a set of memory dispatch IDs for each unrolled instance of an access node.
  * Memory duplicates are tracked based on the instance into their duplicates list.
  * Unrolled instances are tracked by the unrolled IDs (duplicate number) of all surrounding iterators.
  *
  * Writers may be dispatched to multiple physical memory duplicates (physical broadcast)
  * Readers should have at most one dispatch ID.
  *
  * Option:  sym.getDispatches     --- for the entire map
  * Option:  sym.getDispach(uid)   --- for a single unrolled instance id
  * Getter:  sym.dispatches        --- for the entire map
  * Getter:  sym.dispatch(uid)     --- for a single unrolled instance id
  * Setter:  sym.dispatches = (Map[ Seq[Int],Set[Int] ])
  * Default: empty map             --- for the entire map
  * Default: undefined             --- for a single unrolled instance id
  */
case class Dispatch(m: Map[Seq[Int],Set[Int]]) extends StableData[Dispatch]


/** Map of buffer ports for each unrolled instance of an access node.
  * Unrolled instances are tracked by the unrolled IDs (duplicate number) of all surrounding iterators.
  *
  * Option:  sym.getPorts     --- for the entire map
  * Option:  sym.getPort(uid) --- for a single unrolled instance id
  * Getter:  sym.ports        --- for the entire map
  * Getter:  sym.port(uid)    --- for a single unrolled instance id
  * Setter:  sym.ports = (Map[Seq[Int],Port])
  * Default: empty map        --- for the entire map
  * Default: undefined        --- for a single unrolled instance id
  */
case class Ports(m: Map[Seq[Int],Port]) extends StableData[Ports]


/** Flag set by the user to allow buffered writes across metapipeline stages.
  *
  * Getter:  sym.isWriteBuffer
  * Setter:  sym.isWriteBuffer = (true | false)
  * Default: false
  */
case class EnableWriteBuffer(flag: Boolean) extends StableData[EnableWriteBuffer]


trait BankingData {

  implicit class BankedMemoryOps(s: Sym[_]) {
    def isWriteBuffer: Boolean = metadata[EnableWriteBuffer](s).exists(_.flag)
    def isWriteBuffer_=(flag: Boolean): Unit = metadata.add(s, EnableWriteBuffer(flag))

    /** Pre-unrolling duplicates (one or more Memory instances per node) */

    def getDuplicates: Option[Seq[Memory]] = metadata[Duplicates](s).map(_.d)
    def duplicates: Seq[Memory] = getDuplicates.getOrElse{throw new Exception(s"No duplicates defined for $s")}
    def duplicates_=(ds: Seq[Memory]): Unit = metadata.add(s, Duplicates(ds))


    /** Post-unrolling duplicates (exactly one Memory instance per node) */

    def getInstance: Option[Memory] = getDuplicates.flatMap(_.headOption)
    def instance: Memory = getInstance.getOrElse{throw new Exception(s"No instance defined for $s")}
    def instance_=(inst: Memory): Unit = metadata.add(s, Duplicates(Seq(inst)))
  }

  implicit class BankedAccessOps(s: Sym[_]) {

    def getDispatches: Option[Map[Seq[Int], Set[Int]]] = metadata[Dispatch](s).map(_.m)
    def dispatches: Map[Seq[Int], Set[Int]] = getDispatches.getOrElse{ Map.empty }
    def dispatches_=(ds: Map[Seq[Int],Set[Int]]): Unit = metadata.add(s, Dispatch(ds))
    def getDispatch(uid: Seq[Int]): Option[Set[Int]] = getDispatches.flatMap(_.get(uid))
    def dispatch(uid: Seq[Int]): Set[Int] = getDispatch(uid).getOrElse{throw new Exception(s"No dispatch defined for $s {${uid.mkString(",")}}")}

    def getPorts: Option[Map[Seq[Int],Port]] = metadata[Ports](s).map(_.m)
    def ports: Map[Seq[Int],Port] = getPorts.getOrElse{ Map.empty }
    def ports_=(ports: Map[Seq[Int],Port]): Unit = metadata.add(s, Ports(ports))

    def getPort(uid: Seq[Int]): Option[Port] = getPorts.flatMap(_.get(uid))
    def port(uid: Seq[Int]): Port = getPort(uid).getOrElse{ throw new Exception(s"No ports defined for $s {${uid.mkString(",")}}") }
  }

}