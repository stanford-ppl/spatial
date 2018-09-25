package spatial.metadata.memory

import argon._
import forge.tags._

import spatial.targets.MemoryResource

import spatial.metadata.access.AccessMatrix
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.util.{IntLike, spatialConfig}

import utils.implicits.collections._


/** Abstract class for any banking strategy. */
sealed abstract class Banking {
  def nBanks: Int
  def stride: Int
  def dims: Seq[Int]
  def alphas: Seq[Int]
  def Ps: Seq[Int]
  @api def bankSelect[I:IntLike](addr: Seq[I]): I
}

/** Banking address function (alpha*A / B) mod N. */
case class ModBanking(N: Int, B: Int, alpha: Seq[Int], dims: Seq[Int], P: Seq[Int]) extends Banking {
  override def nBanks: Int = N
  override def stride: Int = B
  override def alphas: Seq[Int] = alpha
  override def Ps: Seq[Int] = P

  @api def bankSelect[I:IntLike](addr: Seq[I]): I = {
    import spatial.util.IntLike._
    (alpha.zip(addr).map{case (a,i) => a*i }.sumTree / B) % N
  }
  override def toString: String = {
    val name = if (B == 1) "Cyclic" else "Block Cyclic"
    s"Dims {${dims.mkString(",")}}: $name: N=$N, B=$B, alpha=<${alpha.mkString(",")}>, P=<${P.mkString(",")}>"
  }
}
object ModBanking {
  def Unit(rank: Int) = ModBanking(1, 1, Seq.fill(rank)(1), Seq.tabulate(rank){i => i}, Seq.fill(rank)(1))
  def Simple(banks: Int, dims: Seq[Int], stride: Int) = ModBanking(banks, 1, Seq.fill(dims.size)(1), dims, Seq.fill(dims.size)(stride))
}


/** Helper structure for holding metadata for buffer ports
  * The mux port is the time multiplexed slot the associated access has in reference to all other accesses
  * on the same port occurring at the same time.
  *
  * The buffer port is the port at which the associated access should be connected for N-buffers.
  * If the buffer is depth 1, this is always Some(0).
  * If the access occurs outside of the metapipeline that uses this buffer, the port will be None.
  *
  *            |--------------|--------------|
  *            |   Buffer 0   |   Buffer 1   |
  *            |--------------|--------------|
  * bufferPort         0              1           The buffer port (None for access outside pipeline)
  *                 |x x x|        |x x x|
  *
  *                /       \      /       \
  *
  *              |x x x|x x|     |x x x|x x x|
  * muxPort         0    1          0     1       The ID for the given time multiplexed vector
  *
  *              |( ) O|O O|    |(   )|( ) O|
  * muxOfs        0   2 0 1        0    0  2      Start offset into the time multiplexed vector
  *
  */
case class Port(
  bufferPort: Option[Int],  // The ID for a buffered access (None if time-multiplexed with buffer)
  muxPort: Int,             // The ID on the multiplexed mux for a single port
  muxOfs:  Int,             // The offset of the first element of this access within a mux port
  castgroup: Seq[Int],      // The broadcast group(s) the access belongs to within this port
  broadcast: Seq[Int]       // The broadcast index of this group (0 = broadcasting, >0 = receiving)
)


/** Used during memory analysis to track intermediate results. */
case class Instance(
  reads:    Set[Set[AccessMatrix]], // All reads within this group
  writes:   Set[Set[AccessMatrix]], // All writes within this group
  ctrls:    Set[Ctrl],              // Set of controllers these accesses are in
  metapipe: Option[Ctrl],           // Controller if at least some accesses require n-buffering
  banking:  Seq[Banking],           // Banking information
  depth:    Int,                    // Depth of n-buffer
  cost:     Long,                    // Cost estimate of this configuration
  ports:    Map[AccessMatrix,Port], // Buffer ports
  padding:  Seq[Int],               // Padding for memory based on banking
  accType:  AccumType               // Type of accumulator for instance
) {
  def toMemory: Memory = Memory(banking, depth, padding, accType)

  def accesses: Set[Sym[_]] = accessMatrices.map(_.access)
  def accessMatrices: Set[AccessMatrix] = reads.flatten ++ writes.flatten

  override def toString: String = {
    import scala.math.Ordering.Implicits._

    def prtStr(port: Option[Int], grps: Set[Set[AccessMatrix]], tp: String): String = {
      val accesses = grps.flatten.toSeq
                         .filter{a => ports(a).bufferPort == port }    // All accesses on this port

      val head = s"${port.getOrElse("M")} [Type:$tp]:"
      val lines: Seq[String] = Seq(head) ++ {
        accesses.groupBy{a => ports(a).muxPort }.toSeq.sortBy(_._1).flatMap{case (muxPort, matrices) =>
          Seq(s" - Mux Port #$muxPort: ") ++
          matrices.groupBy(_.access).iterator.flatMap(_._2.sortBy(_.unroll))
                .flatMap{ a =>
                  Seq(
                    s"  [Ofs: ${ports(a).muxOfs}] ${stm(a.access)} {${a.unroll.mkString(",")}}",
                    s"      - ${a.access.ctx}: ${a.access.ctx.content.getOrElse("").trim}",
                    s"      - Scope: ${a.access.scope}"
                  )
                }
        }
      }
      lines.mkString("\n")
    }

    val ps = (0 until depth).map{Some(_)} ++ (if (depth > 1) Seq(None) else Nil)

    /*emit(s"  #$i: Banked")
          emit(s"     Resource: ${inst.resource.name}")
          emit(s"     Depth:    $depth")
          emit(s"     Accum:    $isAccum")
          emit(s"     Banks:    $banks <$format>")
          banking.foreach{grp => emit(s"       $grp") }*/

    val format = if (banking.length == 1) "Flat" else "Hierarchical"

    s"""<Banked>
       |Depth:    $depth
       |Padding:  $padding
       |Accum:    $accType
       |Banking:  $banking <$format>
       |Pipeline: ${metapipe.map(_.toString).getOrElse("---")}
       |Ports:""".stripMargin + "\n" +
    ps.map{port => prtStr(port,writes,"WR") + "\n" + prtStr(port,reads,"RD") }.mkString("\n")
  }

}
object Instance {
  def Unit(rank: Int) = Instance(Set.empty,Set.empty,Set.empty,None,Seq(ModBanking.Unit(rank)),1,0,Map.empty,Seq.fill(rank)(0),AccumType.None)
}


/**
  * Abbreviated version of MemoryInstance for use outside memory analysis
  */
case class Memory(
  banking: Seq[Banking],  // Banking information
  depth:   Int,           // Buffer depth
  padding: Seq[Int],      // Padding on each dim
  accType: AccumType      // Flags whether this instance is an accumulator
) {
  var resourceType: Option[MemoryResource] = None
  @stateful def resource: MemoryResource = resourceType.getOrElse(spatialConfig.target.defaultResource)

  def updateDepth(d: Int): Memory = Memory(banking, d, padding, accType)
  def nBanks: Seq[Int] = banking.map(_.nBanks)
  def Ps: Seq[Int] = banking.map(_.Ps).flatten
  def totalBanks: Int = banking.map(_.nBanks).product
  def bankDepth(dims: Seq[Int]): Int = {
    banking.map{bank =>
      val size = bank.dims.map{i => dims(i) }.product
      Math.ceil(size.toDouble / bank.nBanks)    // Assumes evenly divided
    }.product.toInt
  }

  @api def bankSelects[T:IntLike](mem: Sym[_], addr: Seq[T]): Seq[T] = {
    if (banking.lengthIs(mem.sparseRank.length)) {
      banking.zip(addr).map{case(a,b) => a.bankSelect(Seq(b))}
    } else banking.map(_.bankSelect(addr))
  }

  @api def bankOffset[T:IntLike](mem: Sym[_], addr: Seq[T]): T = {
    import spatial.util.IntLike._
    val w = mem.stagedDims.map(_.toInt).zip(padding).map{case(x,y) => x+y}
    val D = mem.sparseRank.length
    val n = banking.map(_.nBanks).product
    if (banking.lengthIs(1)) {
      val b = banking.head.stride
      val alpha = banking.head.alphas
      val P = banking.head.Ps

      val ofschunk = (0 until D).map{t =>
        val xt = addr(t)
        val p = P(t)
        val ofsdim_t = xt / p
        ofsdim_t * w.slice(t+1,D).zip(P.slice(t+1,D)).map{case (x,y) => math.ceil(x/y).toInt}.product
      }.sumTree
      val intrablockofs = (0 until D).map{t => 
        addr(t)
      }.sumTree % b // Appears to be modulo magic but may be wrong
      ofschunk * b + intrablockofs
    }
    else if (banking.lengthIs(D)) {
      val b = banking.map(_.stride)
      val P = banking.map(_.Ps).flatten
      val ofschunk = (0 until D).map{t =>
        val xt = addr(t)
        val p = P(t)
        val ofsdim_t = xt / p
        ofsdim_t * w.slice(t+1,D).zip(P.slice(t+1,D)).map{case (x,y) => math.ceil(x/y).toInt}.product
      }.sumTree
      val intrablockofs = (0 until D).map{t => 
        addr(t) 
      }.sumTree % b.head.toInt // Appears to be modulo magic but may be wrong 
      ofschunk * b.head.toInt + intrablockofs
    }
    else {
      // TODO: Bank address for mixed dimension groups
      throw new Exception("Bank address calculation for arbitrary dim groupings unknown")
    }
  }
}
object Memory {
  def unit(rank: Int): Memory = Memory(Seq(ModBanking.Unit(rank)), 1, Seq.fill(rank)(0), AccumType.None)
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
case class Duplicates(d: Seq[Memory]) extends Data[Duplicates](Transfer.Mirror)


/** Padding on memory per dimension so that offset calculation works out
  * Option:  sym.getPadding
  * Getter:  sym.padding
  * Setter:  sym.padding = (Seq[Int])
  * Default: undefined
  */
case class Padding(dims: Seq[Int]) extends Data[Padding](SetBy.Analysis.Self)


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
case class Dispatch(m: Map[Seq[Int],Set[Int]]) extends Data[Dispatch](Transfer.Mirror)


/** Map of buffer ports for each unrolled instance of an access node.
  * Unrolled instances are tracked by the unrolled IDs (duplicate number) of all surrounding iterators.
  *
  * Option:  sym.getPorts        --- for the entire map
  * Option:  sym.getPort(uid)    --- for a single unrolled instance id
  * Getter:  sym.ports(disp)     --- for the entire map
  * Getter:  sym.port(disp,uid)  --- for a single unrolled instance id
  * Setter:  sym.addPort(Int, Seq[Int, Port)
  * Default: empty map           --- for the entire map
  * Default: undefined           --- for a single unrolled instance id
  */
case class Ports(m: Map[Int, Map[Seq[Int],Port]]) extends Data[Ports](Transfer.Mirror)


/** Flag set by the user to allow buffered writes across metapipeline stages.
  *
  * Getter:  sym.isWriteBuffer
  * Setter:  sym.isWriteBuffer = (true | false)
  * Default: false
  */
case class EnableWriteBuffer(flag: Boolean) extends Data[EnableWriteBuffer](SetBy.User)

/** Flag set by the user to disable buffering caused by accesses over metapipeline stages.
  * Used for manually creating a memory that behaves like a line-buffer
  *
  * Getter:  sym.isWriteBuffer
  * Setter:  sym.isWriteBuffer = (true | false)
  * Default: false
  */
case class EnableNonBuffer(flag: Boolean) extends Data[EnableNonBuffer](SetBy.User)

/** Flag set by the user to disable flattened banking and only attempt hierarchical banking,
  * Used in cases where it could be tricky to find flattened scheme but hierarchical scheme 
  * is very simple
  *
  * Getter:  sym.isHierarchicalBank
  * Setter:  sym.isHierarchicalBank = (true | false)
  * Default: false
  */
case class HierarchicalBank(flag: Boolean) extends Data[HierarchicalBank](SetBy.User)

/** Flag set by the user to disable hierarchical banking and only attempt flat banking,
  * Used in cases where it could be tricky or impossible to find hierarchical scheme but 
  * user knows that a flat scheme exists or is a simpler search
  *
  * Getter:  sym.isFlatBank
  * Setter:  sym.isFlatBank = (true | false)
  * Default: false
  */
case class FlatBank(flag: Boolean) extends Data[FlatBank](SetBy.User)

/** Flag set by the user to ensure an SRAM will merge the buffers, in cases
    where you have metapipelined access such as pre-load, accumulate, store.
  *
  * Getter:  sym.shouldCoalesce
  * Setter:  sym.shouldCoalesce = (true | false)
  * Default: false
  */
case class ShouldCoalesce(flag: Boolean) extends Data[ShouldCoalesce](SetBy.User)
