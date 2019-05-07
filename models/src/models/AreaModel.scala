package models

// import java.nio.channels.{FileLock, OverlappingFileLockException}
// import java.nio.file.StandardOpenOption.{CREATE_NEW, WRITE}
// import java.io.PrintWriter
// import utils.process.BackgroundProcess
// import scala.language.postfixOps

import scala.collection.mutable.HashMap
import org.jpmml.evaluator._
import scala.collection.JavaConverters._
import _root_.java.io.File
import org.dmg.pmml.FieldName

/**
  * Self-initializing object that runs external polyhedral ISL processes
  */
class AreaEstimator {
  val openedModels: HashMap[(String,String), Evaluator] = HashMap()

  // private lazy val proc = {
  //   val user_bin = s"""${sys.env.getOrElse("HOME", "")}/bin"""
  //   val bin_path = java.nio.file.Paths.get(user_bin)
  //   val bin_exists = java.nio.file.Files.exists(bin_path)
  //   if (!bin_exists) {
  //     java.nio.file.Files.createDirectories(bin_path)
  //   }

  //   val estimate_bin = s"""${user_bin}/estimate_area.py"""
  //   val estimate_path = java.nio.file.Paths.get(estimate_bin)

  //   {
  //     // step 1: check if estimate_area.py`s exists && is correct version
  //     val source_string = {
  //       try { 
  //         val raw = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("estimate_area.py")).mkString        
  //         // Clean up the python to make it friendly with bash echo -e
  //         val clean = raw.split("\n").map{x => val indents = x.split("").takeWhile(x => x == " " || x == "\t").size; "    "*indents + x.drop(indents)}.mkString("\n")
  //         clean
  //       } catch {
  //         case _: Throwable => throw new Exception("Could not get estimate_area.py source code")
  //       }
  //     }
      
  //     val estimate_exists = java.nio.file.Files.exists(estimate_path)
  //     import sys.process._
  //     val needsCompile = try {
  //       if (estimate_exists) {
  //         val binVersion = s"$estimate_bin version" !!
  //         val verFromBin = raw"Version: (\d)\.(\d)".r
  //         val verFromBin(binDec,binFrac) = binVersion.split("\n").filter(_.contains("Version:")).head
  //         val curVersion = source_string
  //         val verFromCode = raw".*version[ ]+=[ ]+(\d)\.(\d).*".r
  //         val verFromCode(curDec,curFrac) = source_string.split("\n").filter(_.contains("float version =")).head
  //         println(s"estimate_area (${estimate_path}): Installed Version = $binDec.$binFrac, Required Version = $curDec.$curFrac")
  //         binDec != curDec || binFrac != curFrac
  //       } else {true}
  //     } catch {
  //       case e: Throwable => println("Unsure if estimate needs to be recompiled... Recompiling to be safe"); true 
  //     }
      

  //     try {
  //       if (needsCompile) {
  //         println(source_string)
  //         new PrintWriter(estimate_bin) { write(source_string); close }
  //         println("Finished Compiling")
  //       }

  //       // val sampleArea = s"""estimate_area.py sample""" !!

  //       // println(s"AreaEstimator test passed! $sampleArea")

  //     } catch {
  //       case _: Throwable => throw new Exception("Error compiling estimate_area.py")
  //     }
  //   }

  //   BackgroundProcess("", "estimate_area.py")
  // }

  private var needsInit: Boolean = true

  implicit def area: AreaEstimator = this

  private def init(): Unit = if (needsInit) {
    // proc.run()
    needsInit = false
  }

  def startup(): Unit = init()

  def shutdown(wait: Long = 0): Unit = {
    // proc.kill(wait)
  }

  // This needs to be defined by the instantiator
  def estimate(prop: String, nodetype: String, values: Seq[Int]): Double = {


    val path = "/home/tianzhao/area_analyzer/pmmls/"
    val modelName = nodetype + "_" + prop + ".pmml"
    val evaluator: Evaluator = openedModels.getOrElseUpdate((nodetype,prop), {
      val e = new LoadingModelEvaluatorBuilder().load(new File(path + modelName))build()
      e.verify()
      e
    })

    // Java part
    val inputFields: Array[InputField] = evaluator.getInputFields.toArray.map(f => f.asInstanceOf[InputField])
    val fieldNames: Array[FieldName] = inputFields.map(f => f.getName)
    val fieldValues: Array[FieldValue] = (inputFields zip values).map {
      case (f, d) =>
        f.prepare(d)
    }
    val args = (fieldNames zip fieldValues).toMap
    val targetFields = evaluator.evaluate(args.asJava).toString
    val resultExtractor = raw".*result=(\d+)\.(\d+).*".r
    val resultExtractor(dec,frac) = targetFields
    val result = (dec + "." + frac).toDouble
    result

    // proc.send(s"$prop $nodetype ${values.mkString(" ")}")
    // val response = proc.blockOnChar()

    // if (response == '0') 1.0
    // else if (response == '1') 2.0
    // else throw new Exception("Failed area estimate check")

  }
}
