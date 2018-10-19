package spatial.dse

import argon._
import spatial.metadata.control._
import spatial.metadata.params._
import spatial.node._
import spatial.lang.I32
import spatial.metadata.bounds._
import spatial.metadata.types._

import java.io.PrintWriter
import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, TimeUnit}

trait DSEAnalyzer extends argon.passes.Traversal {// with SpaceGenerator with HyperMapperDSE {

  // /*abstract class SearchMode
  // case object BruteForceSearch extends SearchMode
  // case object HeuristicSearch  extends SearchMode
  // final val searchMode: SearchMode = HeuristicSearch*/
  // def restricts: Set[Restrict]
  // def tileSizes: Set[Param[Index]]
  // def parFactors: Set[Param[Index]]
  // def localMems: Seq[Exp[_]]
  // def metapipes: Seq[Exp[_]]
  // def top: Exp[_]

  // override protected def process[S: Type](block: Block[S]): Block[S] = {
  //   if (spatialConfig.enableDSE) {
  //     report("Tile sizes: ")
  //     tileSizes.foreach{t => report(u"  $t (${t.ctx})")}
  //     report("Parallelization factors:")
  //     parFactors.foreach{p => report(u"  $p (${p.ctx})")}
  //     report("Metapipelining toggles:")
  //     metapipes.foreach{m => report(u"  $m (${m.ctx})")}

  //     val intParams = (tileSizes ++ parFactors).toSeq
  //     val intSpace = createIntSpace(intParams, restricts)
  //     val ctrlSpace = createCtrlSpace(metapipes)
  //     val params = intParams ++ metapipes
  //     val space = intSpace ++ ctrlSpace

  //     report("Space: ")
  //     params.zip(space).foreach{case (p,d) => report(u"  $p: $d (${p.ctx})") }

  //     if (spatialConfig.bruteForceDSE) bruteForceDSE(params, space, block)
  //     else if (spatialConfig.heuristicDSE) heuristicDSE(params, space, restricts, block)
  //     else if (spatialConfig.hyperMapperDSE) hyperMapperDSE(space, block)
  //   }
  //   dbg("Freezing parameters")
  //   tileSizes.foreach{t => t.makeFinal() }
  //   parFactors.foreach{p => p.makeFinal() }
  //   block
  // }



  // def heuristicDSE(params: Seq[Exp[_]], space: Seq[Domain[_]], restrictions: Set[Restrict], program: Block[_]): Unit = {
  //   val EXPERIMENT = spatialConfig.experimentDSE
  //   report("Intial Space Statistics: ")
  //   report("-------------------------")
  //   report(s"  # of parameters: ${space.size}")
  //   report(s"  # of points:     ${space.map(d => BigInt(d.len)).product}")
  //   report("")

  //   report("Found the following space restrictions: ")
  //   restrictions.foreach{r => report(s"  $r") }

  //   val prunedSpace = space.zip(params).map{
  //     case (domain, p: Param[_]) =>
  //       val relevant = restrictions.filter(_.dependsOnlyOn(p))
  //       domain.filter{state => relevant.forall(_.evaluate()(state)) }

  //     case (domain, _) => domain
  //   }
  //   val indexedSpace = prunedSpace.zipWithIndex
  //   val N = prunedSpace.length
  //   val T = spatialConfig.threads
  //   val dims = prunedSpace.map{d => BigInt(d.len) }
  //   val prods = List.tabulate(N){i => dims.slice(i+1,N).product }
  //   val NPts = dims.product

  //   report("")
  //   report("Pruned space: ")
  //   params.zip(prunedSpace).foreach{case (p,d) => report(u"  $p: $d (${p.ctx})") }

  //   val restricts = restrictions.filter(_.deps.size > 1)
  //   def isLegalSpace(): Boolean = restricts.forall(_.evaluate())

  //   if (NPts < Int.MaxValue) {
  //     //val legalPoints = ArrayBuffer[BigInt]()

  //     val legalStart = System.currentTimeMillis
  //     println(s"Enumerating ALL legal points...")
  //     //val startTime = System.currentTimeMillis
  //     //var nextNotify = 0.0; val notifyStep = NPts.toInt/10
  //     /*for (i <- 0 until NPts.toInt) {
  //       indexedSpace.foreach{case (domain,d) => domain.set( ((i / prods(d)) % dims(d)).toInt ) }
  //       if (isLegalSpace()) legalPoints += i

  //       if (i > nextNotify) {
  //         val time = System.currentTimeMillis - startTime
  //         println("%.4f".format(100*(i/NPts.toFloat)) + s"% ($i / $NPts) Complete after ${time/1000} seconds (${legalPoints.size} / $i valid so far)")
  //         nextNotify += notifyStep
  //       }
  //     }*/

  //     val workerIds = (0 until T).toList
  //     val pool = Executors.newFixedThreadPool(T)
  //     val results = new LinkedBlockingQueue[Seq[Int]](T)
  //     val BLOCK_SIZE = Math.ceil(NPts.toDouble / T).toInt
  //     println(s"Number of candidate points is: ${NPts}")
  //     report(s"Using $T threads with block size of $BLOCK_SIZE")

  //     workerIds.foreach{i =>
  //       val start = i * BLOCK_SIZE
  //       val size = Math.min(NPts.toInt - BLOCK_SIZE, BLOCK_SIZE)
  //       val threadState = new State
  //       state.copyTo(threadState)
  //       val worker = PruneWorker(
  //         start,
  //         size,
  //         prods,
  //         dims,
  //         indexedSpace,
  //         restricts,
  //         results
  //       )(threadState)
  //       pool.submit(worker)
  //     }

  //     pool.shutdown()
  //     pool.awaitTermination(10L, TimeUnit.HOURS)

  //     val legalPoints = workerIds.flatMap{_ => results.take() }.map{i => BigInt(i)}

  //     val legalCalcTime = System.currentTimeMillis - legalStart
  //     val legalSize = legalPoints.length
  //     println(s"Legal space size is $legalSize (elapsed time: ${legalCalcTime/1000} seconds)")
  //     println("")

  //     val nTrials = 10

  //     if (EXPERIMENT) {
  //       //val times = new PrintWriter(config.name + "_times.log")
  //       //val last = if (legalSize < 100000) Seq(legalSize) else Nil
  //       //val sizes = List(500, 1000, 5000, 10000, 20000, 50000, 75000, 100000) ++ last
  //       //sizes.filter(_ <= legalSize).foreach{size =>
  //       val size = Math.min(legalSize, 100000)
  //       (0 until nTrials).foreach{i =>
  //         val points = scala.util.Random.shuffle(legalPoints).take(size)
  //         val filename = s"${config.name}_trial_$i.csv"

  //         val startTime = System.currentTimeMillis()
  //         val BLOCK_SIZE = Math.min(Math.ceil(size.toDouble / T).toInt, 500)

  //         threadBasedDSE(points.length, params, prunedSpace, program, file = filename, overhead = legalCalcTime) { queue =>
  //           points.sliding(BLOCK_SIZE, BLOCK_SIZE).foreach { block =>
  //             //println(s"[Master] Submitting block of length ${block.length} to work queue")
  //             queue.put(block)
  //           }
  //         }

  //         val endTime = System.currentTimeMillis()
  //         val totalTime = (endTime - startTime)
  //         println(s"${config.name} ${i}: $totalTime")
  //       }
  //       //}
  //       //times.close()
  //       println("All experiments completed. Exiting.")
  //       sys.exit(0)
  //     }
  //     else {
  //       val points = scala.util.Random.shuffle(legalPoints).take(75000)
  //       val BLOCK_SIZE = Math.min(Math.ceil(points.size.toDouble / T).toInt, 500)

  //       threadBasedDSE(points.length, params, prunedSpace, program) { queue =>
  //         points.sliding(BLOCK_SIZE, BLOCK_SIZE).foreach { block =>
  //           queue.put(block)
  //         }
  //       }
  //     }
  //   }
  //   else {
  //     error("Space size is greater than Int.MaxValue. Don't know what to do here yet...")
  //   }
  // }

  // // P: Total space size
  // def threadBasedDSE(P: BigInt, params: Seq[Exp[_]], space: Seq[Domain[_]], program: Block[_], file: String = config.name+"_data.csv", overhead: Long = 0L)(pointGen: BlockingQueue[Seq[BigInt]] => Unit): Unit = {
  //   val names = params.map{p => p.name.getOrElse(p.toString) }
  //   val N = space.size
  //   val T = spatialConfig.threads
  //   val dir = if (config.resDir.startsWith("/")) config.resDir + "/" else config.cwd + s"/${config.resDir}/"
  //   val filename = dir + file
  //   val BLOCK_SIZE = Math.min(Math.ceil(P.toDouble / T).toInt, 500)

  //   new java.io.File(dir).mkdirs()

  //   report("Space Statistics: ")
  //   report("-------------------------")
  //   report(s"  # of parameters: $N")
  //   report(s"  # of points:     $P")
  //   report("")
  //   report(s"Using $T threads with block size of $BLOCK_SIZE")
  //   report(s"Writing results to file $filename")

  //   val workQueue = new LinkedBlockingQueue[Seq[BigInt]](5000)  // Max capacity specified here
  //   val fileQueue = new LinkedBlockingQueue[Array[String]](5000)

  //   val workerIds = (0 until T).toList

  //   val pool = Executors.newFixedThreadPool(T)
  //   val writePool = Executors.newFixedThreadPool(1)

  //   val workers = workerIds.map{id =>
  //     val threadState = new State
  //     state.copyTo(threadState)
  //     DSEThread(
  //       threadId  = id,
  //       params    = params,
  //       space     = space,
  //       accel     = top,
  //       program   = program,
  //       localMems = localMems,
  //       workQueue = workQueue,
  //       outQueue  = fileQueue
  //     )(threadState)
  //   }
  //   report("Initializing models...")

  //   // Initializiation may not be threadsafe - only creates 1 area model shared across all workers
  //   workers.foreach{worker => worker.init() }

  //   //val superHeader = List.tabulate(names.length){i => if (i == 0) "INPUTS" else "" }.mkString(",") + "," +
  //   //  List.tabulate(workers.head.areaHeading.length){i => if (i == 0) "OUTPUTS" else "" }.mkString(",") + ", ,"
  //   val header = names.mkString(",") + "," + workers.head.areaHeading.mkString(",") + ", Cycles, Valid"

  //   val writer = DSEWriterThread(
  //     threadId  = T,
  //     spaceSize = P,
  //     filename  = filename,
  //     header    = header,
  //     workQueue = fileQueue
  //   )

  //   report("And aaaawaaaay we go!")

  //   workers.foreach{worker => pool.submit(worker) }
  //   writePool.submit(writer)

  //   // Submit all the work to be done
  //   // Work queue blocks this thread when it's full (since we'll definitely be faster than the worker threads)
  //   val startTime = System.currentTimeMillis
  //   workers.foreach{worker => worker.START = startTime - overhead }

  //   pointGen(workQueue)
  //   /*var i = BigInt(0)
  //   while (i < P) {
  //     val len: Int = if (P - i < BLOCK_SIZE) (P - i).toInt else BLOCK_SIZE
  //     if (len > 0) workQueue.put((i,len))
  //     i += BLOCK_SIZE
  //   }*/

  //   println("[Master] Ending work queue.")

  //   // Poison the work queue (make sure to use enough to kill them all!)
  //   workerIds.foreach{_ => workQueue.put(Seq.empty[BigInt]) }

  //   println("[Master] Waiting for workers to complete...")

  //   // Wait for all the workers to die (this is a fun metaphor)
  //   pool.shutdown()
  //   pool.awaitTermination(10L, TimeUnit.HOURS)
  //   println("[Master] Waiting for file writing to complete...")

  //   // Poison the file queue too and wait for the file writer to die
  //   fileQueue.put(Array.empty[String])
  //   writePool.shutdown()
  //   writePool.awaitTermination(10L, TimeUnit.HOURS)

  //   val endTime = System.currentTimeMillis()
  //   val totalTime = (endTime - startTime)/1000.0

  //   if (PROFILING) {
  //     val bndTime = workers.map(_.bndTime).sum
  //     val memTime = workers.map(_.memTime).sum
  //     val conTime = workers.map(_.conTime).sum
  //     val areaTime = workers.map(_.areaTime).sum
  //     val cyclTime = workers.map(_.cyclTime).sum
  //     val total = bndTime + memTime + conTime + areaTime + cyclTime
  //     println("Profiling results: ")
  //     println(s"Combined runtime: $total")
  //     println(s"Scalar analysis:     $bndTime"  + " (%.3f)".format(100*bndTime.toDouble/total) + "%")
  //     println(s"Memory analysis:     $memTime"  + " (%.3f)".format(100*memTime.toDouble/total) + "%")
  //     println(s"Contention analysis: $conTime"  + " (%.3f)".format(100*conTime.toDouble/total) + "%")
  //     println(s"Area analysis:       $areaTime" + " (%.3f)".format(100*areaTime.toDouble/total) + "%")
  //     println(s"Runtime analysis:    $cyclTime" + " (%.3f)".format(100*cyclTime.toDouble/total) + "%")
  //   }

  //   println(s"[Master] Completed space search in $totalTime seconds.")
  // }

  // def bruteForceDSE(params: Seq[Exp[_]], space: Seq[Domain[_]], program: Block[_]): Unit = {
  //   val P = space.map{d => BigInt(d.len) }.product
  //   val T = spatialConfig.threads
  //   val BLOCK_SIZE = Math.min(Math.ceil(P.toDouble / T).toInt, 500)

  //   threadBasedDSE(P, params, space, program){queue =>
  //     var i = BigInt(0)
  //     while (i < P) {
  //       val len: Int = if (P - i < BLOCK_SIZE) (P - i).toInt else BLOCK_SIZE
  //       if (len > 0) queue.put(i until i + len)
  //       i += BLOCK_SIZE
  //     }
  //   }
  // }

}
