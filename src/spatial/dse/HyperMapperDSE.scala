package spatial.dse

import java.io.PrintStream
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import argon._
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
import spatial.util.spatialConfig

trait HyperMapperDSE { this: DSEAnalyzer =>

  def hyperMapperDSE(space: Seq[Domain[_]], program: Block[_], file: String = config.name + "_data.csv"): Unit = {
    val N = space.size
    val T = spatialConfig.threads
    val dir = if (config.genDir.startsWith("/")) config.genDir + "/" else config.cwd + s"/${config.genDir}/"
    val filename = dir + file

    new java.io.File(dir).mkdirs()

    dbgs("Space Statistics: ")
    dbgs("-------------------------")
    dbgs(s"  # of parameters: $N")
    dbgs("")
    dbgs(s"Using $T threads")
    dbgs(s"Writing results to file $filename")

    val workQueue = new LinkedBlockingQueue[Seq[Any]](5000)  // Max capacity specified here
    val fileQueue = new LinkedBlockingQueue[String](5000)

    val workerIds = (0 until T).toList

    val pool = Executors.newFixedThreadPool(T)

    val pcsFile = config.name + ".pcs"
    val jsonFile = config.name + ".json"
    val workDir = config.cwd + "/dse_hm"

    println("Creating PCS file")
    withLog(workDir, pcsFile){
      space.foreach{domain =>
        msg(s"""${domain.name} ${domain.tp} {${domain.options.mkString(", ")}}""", 100)
      }
    }
    println("Creating Hypermapper config JSON file")
    withLog(workDir, jsonFile){
      msg(s"""{
             |  "application_name": "${config.name}",
             |  "pcs_file": "$workDir/$pcsFile",
             |  "max_number_of_predictions": 1000000,
             |  "max_number_AL_iterations": 5,
             |  "number_of_repetitions": 1,
             |  "number_of_cpus": 6,
             |  "hypermapper_mode": {
             |    "mode": "interactive"
             |  },
             |  "optimization_objectives": ["ALMs", "Cycles"],
             |  "feasible_output": {
             |    "name": "Valid",
             |    "true_value": "true",
             |    "false_value": "false"
             |  },
             |  "timestamp": "Timestamp",
             |  "max_runs_in_one_AL_iteration": 100,
             |  "run_directory": "$dir",
             |  "output_data_file": "${config.name}_output_dse_samples.csv",
             |  "output_pareto_file": "${config.name}_output_pareto.csv",
             |  "number_of_startup_random_sampling": 1000,
             |  "output_image": {
             |    "output_image_pdf_file": "${config.name}_output_pareto.pdf",
             |    "optimization_objectives_labels_image_pdf": ["Logic Utilization (%)", "Cycles (log)"],
             |    "image_xlog": false,
             |    "image_ylog": true,
             |    "objective_1_max": 262400
             |  }
             |}""".stripMargin)
    }

    case class SpatialError(t: Throwable) extends Throwable

    val start = System.currentTimeMillis()
    val workers = workerIds.map{id =>
      val threadState = new State(state.app)
      implicit val isl = this.isl
      this.IR.copyTo(threadState)
      HyperMapperThread(
        threadId  = id,
        start     = start,
        space     = space,
        accel     = TopCtrl.get,
        program   = program,
        localMems = LocalMemories.all.toSeq,
        workQueue = workQueue,
        outQueue  = fileQueue
      )(threadState, isl, areamodel)
    }
    val HEADER = space.map(_.name).mkString(",") + "," + workers.head.areaHeading.mkString(",") + ",Cycles,Valid,Timestamp"

    val HYPERMAPPER: String = sys.env.getOrElse("HYPERMAPPER_HOME", {error(ctx, "Please set the HYPERMAPPER_HOME environment variable."); sys.exit() })
    Console.println(s"python ${HYPERMAPPER}/scripts/hypermapper.py $workDir/$jsonFile")
    val hm = Subproc("python", HYPERMAPPER + "/scripts/hypermapper.py", workDir + "/" + jsonFile) { (cmd,reader) =>
      if ((cmd ne null) && !cmd.startsWith("Pareto")) { // TODO
        try {
          println(s"[Master] Received Line: $cmd")
          val parts = cmd.split(" ").map(_.trim)
          val command = parts.head

          command match {
            case "Request" =>
              val nPoints = parts.last.toInt
              val head    = reader.readLine()
              val header  = head.split(",").map(_.trim)
              val order   = space.map{d => header.indexOf(d.name) }
              if (order.exists(_ < 0)) {
                bug(s"[Master] Received Line: $head")
                order.zipWithIndex.filter{case (idx, i) => idx < 0 }.foreach{case (idx, i) =>
                  bug(s"Header was missing: ${space(i).name}")
                }
                throw SpatialError(new Exception(s"Missing header names"))
              }
              val points  = (0 until nPoints).map{i =>
                print(s"[Master] Receiving Point $i: ")
                val pt = reader.readLine()
                println(pt)
                pt
              }

              try {
                println(s"[Master] Received Line: $head")
                points.foreach { point =>
                  println(s"[Master] Received Line: $point")
                  val values = point.split(",").map(_.trim.toLowerCase).map {
                    case "true" => true
                    case "false" => false
                    case x => x.toInt
                  }
                  workQueue.put(order.map { i => values(i) })
                }
                val result = HEADER + "\n" + points.indices.map { _ => fileQueue.take() }.mkString("\n")
                println("[Master] Sending back:")
                println(result)
                Some(result)
              }
              catch {case t: Throwable =>
                bug(s"$cmd")
                bug(s"$head")
                points.foreach{point => bug(s"$point") }
                bug(s"${t.getMessage}")
                throw SpatialError(t)
                None
              }

            case "Pareto" =>
              // TODO: Do something with the pareto
              //val data = new PrintStream(config.name + "_hm_data.csv")
              //data.println(HEADER)
              //points.foreach{pt => data.println(pt) }
              //data.close()
              None
        }}
        catch {
          case SpatialError(e) => throw e
          case t:Throwable =>
            println(s"[Ignored] $cmd")
            println(s"[Ignored] Reason: ${t.getMessage}")
            None
        }
      }
      else None
    }

    // Initializiation may not be threadsafe - only creates 1 area model shared across all workers
    println("Initializing models...")
    workers.foreach{worker => worker.init() }
    println("Starting up workers...")
    workers.foreach{worker => pool.submit(worker) }

    val startTime = System.currentTimeMillis
    println("Starting up HyperMapper...")
    hm.block(Some(workDir))

    println("Ending work queue.")

    // Poison the work queue (make sure to use enough to kill them all!)
    workerIds.foreach{_ => workQueue.put(Seq.empty[Int]) }

    println("Waiting for workers to complete...")
    pool.shutdown()
    pool.awaitTermination(10L, TimeUnit.HOURS)

    val endTime = System.currentTimeMillis()
    val totalTime = (endTime - startTime)/1000.0

    println(s"Completed space search in $totalTime seconds.")
    sys.exit(0) // Bail for now
  }

}
