package spatial.metadata.memory

import argon._
import forge.tags._

import spatial.targets.MemoryResource

import spatial.metadata.access.AccessMatrix
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.util.{IntLike, spatialConfig}
import utils.math._

import utils.implicits.collections._


/** Abstract class for any banking strategy. */
sealed abstract class Banking {
  def nBanks: Int
  def stride: Int
  def dims: Seq[Int]
  def alphas: Seq[Int]
  def Ps: Seq[Int]
  def hiddenVolume: Int
  def numChecks: Int
  def solutionVolume: Int
  @api def bankSelect[I:IntLike](addr: Seq[I]): I
}

/** Banking address function (alpha*A / B) mod N. */
case class ModBanking(N: Int, B: Int, alpha: Seq[Int], dims: Seq[Int], P: Seq[Int], sv: Int = 1, checks: Int = 0) extends Banking {
  override def nBanks: Int = N
  override def stride: Int = B
  override def alphas: Seq[Int] = alpha
  override def Ps: Seq[Int] = P
  override def hiddenVolume: Int = {
    val hang = P.map{_ % N}.min
    if (hang == 0) 0 else B*(N-P.map{_ % N}.min)
  }
  override def numChecks: Int = checks  // Diagnostic for required number of ISL calls to verify scheme
  override def solutionVolume: Int = sv

  @api def bankSelect[I:IntLike](addr: Seq[I]): I = {
    import spatial.util.IntLike._
    (alpha.zip(addr).map{case (a,i) => a*i }.sumTree / B) % N
  }
  override def toString: String = {
    val name = if (B == 1) "Cyclic" else "Block Cyclic"
    s"Dims {${dims.mkString(",")}}: $name: N=$N, B=$B, alpha=<${alpha.mkString(",")}>, P=<${P.mkString(",")}> ($solutionVolume solutions, $numChecks checks)"
  }
}
object ModBanking {
  def Unit(rank: Int, dims: Seq[Int]) = ModBanking(1, 1, Seq.fill(rank)(1), dims, Seq.fill(rank)(1))
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
  cost:     Double,                    // Cost estimate of this configuration
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
  def Unit(rank: Int) = Instance(Set.empty,Set.empty,Set.empty,None,Seq(ModBanking.Unit(rank, Seq.tabulate(rank){i => i})),1,0,Map.empty,Seq.fill(rank)(0),AccumType.None)
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
  def Bs: Seq[Int] = banking.map(_.stride)
  def alphas: Seq[Int] = banking.map(_.alphas).flatten
  def totalBanks: Int = banking.map(_.nBanks).product
  def bankDepth(dims: Seq[Int]): Int = {
    banking.map{bank =>
      val size = if (dims.nonEmpty) bank.dims.map{i => dims(i) }.product else 1
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
    if (banking.lengthIs(1)) {
      val n = banking.map(_.nBanks).product
      val b = banking.head.stride
      val alpha = banking.head.alphas
      val P = banking.head.Ps
      val ofschunk = (0 until D).map{t =>
        val xt = addr(t)
        val p = P(t)
        val ofsdim_t = xt / p
        ofsdim_t * w.slice(t+1,D).zip(P.slice(t+1,D)).map{case (x,y) => math.ceil(x/y).toInt}.product
      }.sumTree
      val intrablockofs = addr.zip(alpha).map{case(x,y) => x*y}.sumTree % b
      ofschunk * b + intrablockofs
    }
    else if (banking.lengthIs(D)) {
      val b = banking.map(_.stride)
      val P = banking.map(_.Ps).flatten
      val alpha = banking.map(_.alphas).flatten

      val ofschunk = (0 until D).map{t =>
        val n = banking.map(_.nBanks).apply(t)
        val xt = addr(t)
        val p = P(t)
        val ofsdim_t = xt / p
        val prevDimsOfs = (t+1 until D).map{i => 
          math.ceil(w(i)/P(i)).toInt
        }.product
        ofsdim_t * prevDimsOfs
      }.sumTree
      val intrablockofs = (0 until D).map{t =>
        val bAbove = b.slice(t+1,D).product
        bAbove * ((addr(t) * alpha(t)) % b(t))
      }.sumTree
      ofschunk * b.product + intrablockofs
    }
    else {
      // TODO: Bank address for mixed dimension groups
      throw new Exception("Bank address calculation for arbitrary dim groupings unknown")
    }
  }
}
object Memory {
  def unit(rank: Int): Memory = Memory(Seq(ModBanking.Unit(rank, Seq.tabulate(rank){i => i})), 1, Seq.fill(rank)(0), AccumType.None)
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

/*
 * Mapping of uid to group id of access during banking analysis. 
 * Annotated on access
 * */
case class GroupId(m: Map[Seq[Int],Set[Int]]) extends Data[GroupId](Transfer.Mirror)

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
  * Getter:  sym.isNoHierarchicalBank
  * Setter:  sym.isNoHierarchicalBank = (true | false)
  * Default: false
  */
case class NoHierarchicalBank(flag: Boolean) extends Data[NoHierarchicalBank](SetBy.User)

/** Flag set by the user to disable checking for block-cyclic banking schemes.  In general,
  * block-cyclic schemes results in fewer banks but more memory overhead, since some physical
  * addresses are inaccessible by the N,B,alpha equations
  *
  * Getter:  sym.noBlockCyclic
  * Setter:  sym.noBlockCyclic = (true | false)
  * Default: false
  */
case class NoBlockCyclic(flag: Boolean) extends Data[NoBlockCyclic](SetBy.User)

/** Flag set by the user to enable for block-cyclic banking schemes only.  
  *
  * Getter:  sym.onlyBlockCyclic
  * Setter:  sym.onlyBlockCyclic = (true | false)
  * Default: false
  */
case class OnlyBlockCyclic(flag: Boolean) extends Data[OnlyBlockCyclic](SetBy.User)

/** Flag set by the user to specify if only certain NStrictnesses should be checked
  *
  * Getter:  sym.nConstraints
  * Setter:  sym.nConstraints = (true | false)
  * Default: false
  */
case class NConstraints(typs: Seq[NStrictness]) extends Data[NConstraints](SetBy.User)

/** Flag set by the user to specify if only certain AlphaStrictnesses should be checked
  *
  * Getter:  sym.alphaConstraints
  * Setter:  sym.alphaConstraints = (true | false)
  * Default: false
  */
case class AlphaConstraints(typs: Seq[AlphaStrictness]) extends Data[AlphaConstraints](SetBy.User)

/** Flag set by the user for list of Bs to search for block cyclic banking schema
  *
  * Getter:  sym.blockCyclicBs
  * Setter:  sym.blockCyclicBs = Seq(bs)
  * Default: Seq(2, 4, 8, 16, 32, 64, 128, 256)
  */
case class BlockCyclicBs(bs: Seq[Int]) extends Data[BlockCyclicBs](SetBy.User)

/** Flag set by the user to disable hierarchical banking and only attempt flat banking,
  * Used in cases where it could be tricky or impossible to find hierarchical scheme but 
  * user knows that a flat scheme exists or is a simpler search
  *
  * Getter:  sym.isFullFission
  * Setter:  sym.isFullFission = (true | false)
  * Default: false
  */
case class OnlyDuplicate(flag: Boolean) extends Data[OnlyDuplicate](SetBy.User)

/** Flag set by the user to disable hierarchical banking and only attempt flat banking,
  * Used in cases where it could be tricky or impossible to find hierarchical scheme but 
  * user knows that a flat scheme exists or is a simpler search
  *
  * Getter:  sym.duplicateOnAxes = Option[Seq[Seq[Int]]]
  * Setter:  sym.duplicateOnAxes = Seq[Seq[Int]]
  * Default: None
  */
case class DuplicateOnAxes(opts: Seq[Seq[Int]]) extends Data[DuplicateOnAxes](SetBy.User)

/** Flag set by the user to disable bank-by-duplication based on the compiler-defined cost-metric. 
  * This assumes that it will find at least one valid (either flat or hierarchical) bank scheme
  *
  * Getter:  sym.isNoFission
  * Setter:  sym.isNoFission = (true | false)
  * Default: false
  */
case class NoDuplicate(flag: Boolean) extends Data[NoDuplicate](SetBy.User)

/** Flag set by the user to disable banking,
  * Used in cases where it could be tricky or impossible to find any banking scheme scheme and
  * the user does not want the compiler to waste time trying
  *
  * Getter:  sym.isNoFlatBank
  * Setter:  sym.isNoFlatBank = (true | false)
  * Default: false
  */
case class NoFlatBank(flag: Boolean) extends Data[NoFlatBank](SetBy.User)

/** Flag set by the user to force banking analysis to merge buffers even if the 
  * compiler thinks it is unsafe.  Use for cases such as where counter start for different lanes
  * is lane-dependent but known to be such that there are no bank conflicts between lanes
  *
  * Getter:  sym.isMustMerge
  * Setter:  sym.isMustMerge = (true | false)
  * Default: false
  */
case class MustMerge(flag: Boolean) extends Data[MustMerge](SetBy.User)

/** Flag set by the user to ensure an SRAM will merge the buffers, in cases
    where you have metapipelined access such as pre-load, accumulate, store.
  *
  * Getter:  sym.shouldCoalesce
  * Setter:  sym.shouldCoalesce = (true | false)
  * Default: false
  */
case class ShouldCoalesce(flag: Boolean) extends Data[ShouldCoalesce](SetBy.User)

/** Flag set by the user to permit FIFOs where the enqs are technically not bankable,
  * based on control structure analysis alone
  *
  * Getter:  sym.shouldIgnoreConflicts
  * Setter:  sym.shouldIgnoreConflicts = (true | false)
  * Default: false
  */
case class IgnoreConflicts(flag: Boolean) extends Data[IgnoreConflicts](SetBy.User)

/** Flag set by the user to specify a specific banking effort to be put into this particular memory
  *
  * Getter:  sym.bankingEffort
  * Setter:  sym.bankingEffort = (Int)
  * Default: spatialConfig.bankingEffort
  */
case class BankingEffort(effort: Int) extends Data[BankingEffort](SetBy.User)

/** Class for holding the priority of a particular banking option (lower is better) */
abstract trait SearchPriority {
  val P: Int
}

/** Container for describing a set of banking options */
case class BankingOptions(view: BankingView, N: NStrictness, alpha: AlphaStrictness, regroup: RegroupDims) {
  def undesired: Boolean = N.isRelaxed || alpha.isRelaxed
}

/** Put each read access matrix in its own group, forcing compiler to only bank for writers and make a new duplicate for
  * every read`
  */
object RegroupHelper {
  def regroupAny(rank: Int): List[RegroupDims] = List.tabulate(rank){i => i}.toSet.subsets.map{x => RegroupDims(x.toList)}.toList
  def regroupAll(rank: Int): List[RegroupDims] = List(RegroupDims(List.tabulate(rank){i => i}))
  def regroupNone: List[RegroupDims] = List(RegroupDims(List()))
}
case class RegroupDims(dims: List[Int]) extends SearchPriority {
  val P = dims.size
}

/** Enumeration of banking views.  Hierarchical means each dimension gets its own bank address.  Flat means all
  * dimensions are flattened and there is only one scalar representing bank address
  */
sealed trait BankingView extends SearchPriority {
  def expand(): Seq[List[Int]]
  def rank: Int
  def complementView: Seq[Int]
} 
case class Flat(rank: Int) extends BankingView {
  val P = 0
  def expand(): Seq[List[Int]] = Seq(List.tabulate(rank){i => i})
  def complementView: Seq[Int] = List()
}
case class Hierarchical(rank: Int, view: Option[List[Int]] = None) extends BankingView {
  val P = 1
  def expand(): Seq[List[Int]] = {
    if (view.isDefined) Seq.tabulate(rank){i => i}.collect{case i if view.get.contains(i) => List(i)}
    else Seq.tabulate(rank){i => List(i)}
  }
  def complementView: Seq[Int] = if (view.isDefined) Seq.tabulate(rank){i => i}.collect{case i if !view.get.contains(i) => i} else Seq()
}

/** Enumeration of how to search for possible number of banks */
sealed trait NStrictness extends SearchPriority {
  def expand(min: Int, max: Int, stagedDims: List[Int], numAccesses: List[Int], axes: Seq[Int]): List[Int]
  def isRelaxed: Boolean
}
case class UserDefinedN(Ns: Seq[Int]) extends NStrictness {
  val P = 9
  def isRelaxed = false
  def expand(min: Int, max: Int, stagedDims: List[Int], numAccesses: List[Int], axes: Seq[Int]): List[Int] = axes.map(Ns).toList
}
case object NPowersOf2 extends NStrictness {
  val P = 1
  def isRelaxed = false
  def expand(min: Int, max: Int, stagedDims: List[Int], numAccesses: List[Int], axes: Seq[Int]): List[Int] = (min to max).filter(isPow2(_)).toList
}
case object NBestGuess extends NStrictness {
  val P = 0
  def isRelaxed = false
  private def factorize(number: Int): List[Int] = {
    List.tabulate(number){i => i + 1}.collect{case i if number % i == 0 => i} 
  }
  private def primeFactorize(number: Int, list: List[Int] = List()): List[Int] = {
    for(n <- 2 to number if number % n == 0) {
      return primeFactorize(number / n, list :+ n)
    }
    list
  }

  def expand(min: Int, max: Int, stagedDims: List[Int], numAccesses: List[Int], axes: Seq[Int]): List[Int] = { 
    (numAccesses.flatMap(factorize(_)) ++ factorize(stagedDims.product)).filter{x => x <= max && x >= min}.distinct.sorted
  }
}
case object NRelaxed extends NStrictness {
  val P = 2
  def isRelaxed = true
  def expand(min: Int, max: Int, stagedDims: List[Int], numAccesses: List[Int], axes: Seq[Int]): List[Int] = (min to max).filter(!isPow2(_)).toList
}

/** Enumeration of how to search for possible alpha vectors
  * The three groups are:
  * 1) All elements must be powers of 2
  * 2) All elements composed of either:
  *    - Number that evenly divides N
  *    - Number that is the result of a product of some combination of stagedDims (Not sure if this increases the likelihood of valid schemes compared to option 1 only)
  *    - Number that is also power of 2
  *    (consider something like 96x3x3x96 sram N = 36, want to try things like alpha = 18,1,3,9)
  * 3) Everything else
  */
sealed trait AlphaStrictness extends SearchPriority {
  import utils.math._
  /** Creates all alpha vectors comprising of only values in the valids list and which are coprime */
  def selectAs(valids: Seq[Int], dim: Int, prev: Seq[Int], rank: Int): Iterator[Seq[Int]] = {
    if (dim < rank) {
      valids.iterator.flatMap{aD => selectAs(valids, dim+1, prev :+ aD, rank) }.filter(coprime)
    }
    else valids.iterator.map{aR => prev :+ aR }.filter(coprime)
  }
  def expand(rank: Int, N: Int, stagedDims: Seq[Int], axes: Seq[Int]): Iterator[Seq[Int]]
  def isRelaxed: Boolean
}
case class UserDefinedAlpha(alphas: Seq[Int]) extends AlphaStrictness {
  val P = 9
  def isRelaxed = false
  def expand(rank: Int, N: Int, stagedDims: Seq[Int], axes: Seq[Int]): Iterator[Seq[Int]] = Iterator(axes.map(alphas))
}
case object AlphaPowersOf2 extends AlphaStrictness {
  val P = 1
  def isRelaxed = false
  def expand(rank: Int, N: Int, stagedDims: Seq[Int], axes: Seq[Int]): Iterator[Seq[Int]] = {
    val possibleAs = (0 to 2*N).filter(x => isPow2(x) || x == 1 || x == 0 || x == N).uniqueModN(N).filter{x => x >= 0 && x <= N}
    selectAs(possibleAs, 1, Nil, rank)
  }
}
case object AlphaBestGuess extends AlphaStrictness {
  val P = 0
  def isRelaxed = false
  private def factorize(number: Int): List[Int] = {
    List.tabulate(number){i => i + 1}.collect{case i if number % i == 0 => i} 
  }
  private def primeFactorize(number: Int, list: List[Int] = List()): List[Int] = {
    for(n <- 2 to number if number % n == 0) {
      return primeFactorize(number / n, list :+ n)
    }
    list
  }
  def expand(rank: Int, N: Int, stagedDims: Seq[Int], axes: Seq[Int]): Iterator[Seq[Int]] = { 
    val accessBased = Seq.tabulate(factorize(N).length){i => factorize(N).combinations(i+1).toList}.flatten.map(_.product).uniqueModN(N)
    val dimBased = Seq.tabulate(stagedDims.length){i => stagedDims.combinations(i+1).toList}.flatten.map(_.product).filter(_ <= N).uniqueModN(N)
    val coprimes = Seq.tabulate(N){i => i}.collect{case i if coprime(Seq(i,N)) => i}
    val possibleAs = (List(0,1) ++ accessBased ++ dimBased ++ coprimes).filter{x => x >= 0 && x <= N}
    selectAs(possibleAs, 1, Nil, rank)
  }
}
case object AlphaRelaxed extends AlphaStrictness {
  val P = 2
  def isRelaxed = true
  def expand(rank: Int, N: Int, stagedDims: Seq[Int], axes: Seq[Int]): Iterator[Seq[Int]] = {
    val possibleAs = (0 to 2*N).uniqueModN(N).filter{x => x >= 0 && x <= N}
    selectAs(possibleAs, 1, Nil, rank).filterNot(_.forall(x => isPow2(x) || x == 1))
  }
}

/** Info set by the user to specify a specific banking scheme to be used
  *
  * Getter:  sym.explicitBanking: Option
  * Setter:  sym.explicitBanking = (Seq[Int], Seq[Int], Seq[Int])
  * Default: None
  */
case class ExplicitBanking(scheme: (Seq[Int], Seq[Int], Seq[Int])) extends Data[ExplicitBanking](SetBy.User)

/** Flag set by the user to specify that a banking scheme must be used even if it is unsafe
  *
  * Getter:  sym.forceExplicitBanking
  * Setter:  sym.forceExplicitBanking = flag
  * Default: false
  */
case class ForceExplicitBanking(flag: Boolean) extends Data[ForceExplicitBanking](SetBy.User)
