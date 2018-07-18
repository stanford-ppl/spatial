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
  * muxSize            3              3           Width of a single time multiplexed vector
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
case class Port(bufferPort: Option[Int], muxPort: Int, muxSize: Int, muxOfs: Int, broadcast: Int)


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

  def accesses: Set[Sym[_]] = accessMatrices.map(_.access)
  def accessMatrices: Set[AccessMatrix] = reads.flatten ++ writes.flatten

  override def toString: String = {
    import scala.math.Ordering.Implicits._

    def prtStr(port: Option[Int], grps: Set[Set[AccessMatrix]], tp: String): String = {
      val accesses = grps.flatten.toSeq
                         .filter{a => ports(a).bufferPort == port }    // All accesses on this port

      val muxSize: Int = accesses.map{a => ports(a).muxSize }.maxOrElse(0)

      val head = s"${port.getOrElse("M")} [Type:$tp, Width:$muxSize]:"
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
       |Accum:    $accType
       |Banking:  $banking <$format>
       |Pipeline: ${metapipe.map(_.toString).getOrElse("---")}
       |Ports:""".stripMargin + "\n" +
    ps.map{port => prtStr(port,writes,"WR") + "\n" + prtStr(port,reads,"RD") }.mkString("\n")
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
    val w = mem.stagedDims.map(_.toInt).zip(mem.getPadding.getOrElse(Seq.fill(mem.stagedDims.size)(0))).map{case(x,y) => x+y}
    val D = mem.seqRank.length
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
        val xt = addr(t)
        val ofsdim_t = xt % b
        ofsdim_t * List.fill(D-t-1)(b).product.toInt
      }.sumTree
      ofschunk * math.pow(b,D).toInt + intrablockofs
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
        val xt = addr(t)
        val ofsdim_t = xt % b(t)
        ofsdim_t * b.slice(t+1,D).product.toInt
      }.sumTree
      ofschunk * b.product.toInt + intrablockofs
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
