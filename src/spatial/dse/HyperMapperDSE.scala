package spatial.dse

import java.io.PrintStream
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import argon._

import utils.process.BackgroundProcess
import spatial.metadata.control._
import spatial.metadata.params._
import spatial.metadata.memory._
import spatial.node._
import spatial.lang.I32
import spatial.metadata.bounds._
import spatial.metadata.types._
import spatial.traversal._
import poly.ISL
import models.AreaEstimator
import spatial.SpatialConfig
import spatial.util.spatialConfig

trait HyperMapperDSE extends argon.passes.Traversal { this: DSEAnalyzer =>
  final val PROFILING = true

  def hyperMapperDSE(params: Seq[Sym[_]], space: Seq[Domain[_]], program: Block[_], file: String = config.name + "_data.csv"): Unit = {

    val names = params.map{p => p.name.getOrElse(p.toString) }
    val N = space.size
    val P = space.map{d => BigInt(d.len) }.product
    val T = spatialConfig.threads
    val dir = if (config.genDir.startsWith("/")) config.genDir + "/" else config.cwd + s"/${config.genDir}/"
    val filename = dir + file

    new java.io.File(dir).mkdirs()

    dbgs("Space Statistics: ")
    dbgs("-------------------------")
    dbgs(s"  # of parameters: $N")
    dbgs(s"  # of points:     $P")
    dbgs("")
    dbgs(s"Using $T threads")
    dbgs(s"Writing results to file $filename")

    println("Space Statistics: ")
    println("-------------------------")
    println(s"  # of parameters: $N")
    println(s"  # of points:     $P")
    println("")
    println(s"Using $T threads")
    println(s"Writing results to file $filename")

    val workQueue    = new LinkedBlockingQueue[Seq[DesignPoint]](5000)  // Max capacity specified here
    val resultQueue  = new LinkedBlockingQueue[Array[String]](5000)
    val requestQueue = new LinkedBlockingQueue[DSERequest](5000)
    val doneQueue    = new LinkedBlockingQueue[Boolean](100) // TODO: Could be better

    val workerIds = (0 until T).toList
    val commPool = Executors.newFixedThreadPool(2)

    val pool = Executors.newFixedThreadPool(T)

    val jsonFile = config.name + ".json"
    val workDir = config.cwd + "/dse_hm"

    println("Creating Hypermapper config JSON file")
    withLog(workDir, jsonFile){
      log(s"{")
      log(s"""  "application_name": "${config.name}",
             |  "models": {
             |    "model": "random_forest",
             |    "number_of_trees": 20
             |  },
             |  "max_number_of_predictions": 1000000,
             |  "optimization_iterations": 5,
             |  "number_of_cpus": 6,
             |  "number_of_repetitions": 1,
             |  "hypermapper_mode": {
             |    "mode": "interactive"
             |  },
             |  "optimization_objectives": ["ALMs", "Cycles"],
             |  "feasible_output": {
             |    "name": "Valid",
             |    "true_value": "true",
             |    "false_value": "false",
             |    "enable_feasible_predictor": true
             |  },
             |  "timestamp": "Timestamp",
             |  "evaluations_per_optimization_iteration": 100,
             |  "run_directory": "$dir",
             |  "output_data_file": "${config.name}_output_dse_samples.csv",
             |  "output_pareto_file": "${config.name}_output_pareto.csv",
             |  "design_of_experiment": {
             |    "doe_type": "random sampling",
             |    "number_of_samples": 10000
             |  },
             |  "output_image": {
             |    "output_image_pdf_file": "${config.name}_output_pareto.pdf",
             |    "optimization_objectives_labels_image_pdf": ["Logic Utilization (%)", "Cycles (log)"],
             |    "image_xlog": false,
             |    "image_ylog": false,
             |    "objective_1_max": 262400
             |  },
             |  "input_parameters": {""".stripMargin)
      space.zipWithIndex.foreach{case (domain, i) =>
        log(s"""    "${domain.name}": {
             |      "parameter_type" : "${domain.tp}",
             |      "values" : [${domain.optionsString}],
             |      "parameter_default" : ${domain.valueString},
             |      "prior" : ${domain.prior}
             |    }${if (i == space.length-1) "" else ","}""".stripMargin)
      }
      log("  }")
      log("}")
    }

    case class SpatialError(t: Throwable) extends Throwable

    val start = System.currentTimeMillis()

    val workers = workerIds.map{id =>
      val threadState = new State(state.app)
      threadState.config = new SpatialConfig
      state.config.asInstanceOf[SpatialConfig].copyTo(threadState.config) // Extra params
      DSEThread(
        threadId  = id,
        params    = params,
        space     = space,
        accel     = TopCtrl.get,
        program   = program,
        localMems = LocalMemories.all.toSeq,
        workQueue = workQueue,
        outQueue  = resultQueue,
        PROFILING = PROFILING
      )(threadState, this.isl, this.mlModel)
    }

    val HEADER = space.map(_.name).mkString(",") + "," + workers.head.areaHeading.mkString(",") + ",Cycles,Valid,Timestamp"

    val hm = BackgroundProcess(workDir, List("python", spatialConfig.HYPERMAPPER + "/scripts/hypermapper.py", workDir + "/" + jsonFile))
    println("Starting up HyperMapper...")
    println(s"python ${spatialConfig.HYPERMAPPER}/scripts/hypermapper.py $workDir/$jsonFile")
    val (hmOutput, hmInput) = hm.run()

    val receiver = HyperMapperReceiver(
      input      = hmOutput,
      workOut    = workQueue,
      requestOut = requestQueue,
      doneOut    = doneQueue,
      space      = space,
      HEADER     = HEADER,
      THREADS    = T,
      DIR        = workDir
    )
    val sender = HyperMapperSender(
      output    = hmInput,
      requestIn = requestQueue,
      resultIn  = resultQueue,
      doneOut   = doneQueue,
      HEADER    = HEADER
    )

    println("Starting up workers...")
    val startTime = System.currentTimeMillis()
    workers.foreach{worker => pool.submit(worker) }
    workers.foreach{worker => worker.START = startTime }
    commPool.submit(receiver)
    commPool.submit(sender)

    val done = doneQueue.take()
    if (done) {
      println("Waiting for workers to complete...")
      pool.shutdown()
      pool.awaitTermination(10L, TimeUnit.HOURS)
      commPool.shutdown()
      commPool.awaitTermination(10L, TimeUnit.HOURS)

      val endTime = System.currentTimeMillis()
      val totalTime = (endTime - startTime)/1000.0

      println(s"Completed space search in $totalTime seconds.")
    }
    else {
      println("Connected process terminated early!")
      pool.shutdownNow()
      commPool.shutdownNow()
      pool.awaitTermination(1L, TimeUnit.MINUTES)
      commPool.awaitTermination(1L, TimeUnit.MINUTES)
      sys.exit(-1) // Bail for now
    }

    if (PROFILING) {
      val bndTime = workers.map(_.bndTime).sum
      val memTime = workers.map(_.memTime).sum
      val conTime = workers.map(_.conTime).sum
      val areaTime = workers.map(_.areaTime).sum
      val cyclTime = workers.map(_.cyclTime).sum
      val total = bndTime + memTime + conTime + areaTime + cyclTime
      println("Profiling results: ")
      println(s"Combined runtime: $total")
      println(s"Scalar analysis:     $bndTime"  + " (%.3f)".format(100*bndTime.toDouble/total) + "%")
      println(s"Memory analysis:     $memTime"  + " (%.3f)".format(100*memTime.toDouble/total) + "%")
      println(s"Contention analysis: $conTime"  + " (%.3f)".format(100*conTime.toDouble/total) + "%")
      println(s"Area analysis:       $areaTime" + " (%.3f)".format(100*areaTime.toDouble/total) + "%")
      println(s"Runtime analysis:    $cyclTime" + " (%.3f)".format(100*cyclTime.toDouble/total) + "%")
    }

    sys.exit(0) // Bail for now

  }

}
