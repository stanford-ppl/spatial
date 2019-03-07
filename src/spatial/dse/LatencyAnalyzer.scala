package spatial.dse

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.util.modeling._
import spatial.traversal._
import spatial.targets._
import java.io.File
import models._
import argon.node._


case class LatencyAnalyzer(IR: State, latencyModel: LatencyModel) extends RerunTraversal with AccelTraversal {
  var cycleScope: List[Double] = Nil
  var intervalScope: List[Double] = Nil
  var totalCycles: Long = 0L

  def getListOfFiles(d: String):List[String] = {
    import java.nio.file.{FileSystems, Files}
    import scala.collection.JavaConverters._
    val dir = FileSystems.getDefault.getPath(d) 
    Files.walk(dir).iterator().asScala.filter(Files.isRegularFile(_)).map(_.toString).toList//.foreach(println)
  }
  
  override def silence(): Unit = {
    super.silence()
  }

  override def rerun(e: Sym[_], blk: Block[_]): Unit = {
    isRerun = true
    preprocess(blk)
    super.rerun(e, blk)
    postprocess(blk)
    isRerun = false
  }

  override protected def preprocess[A](b: Block[A]): Block[A] = {
    import utils.process.BackgroundProcess
    import scala.language.postfixOps
    import java.io.File
    import sys.process._

    val gen_dir = if (config.genDir.startsWith("/")) config.genDir + "/" else config.cwd + s"/${config.genDir}/"
    val modelJar = getListOfFiles(gen_dir + "/model").filter(_.contains("RuntimeModel-assembly")).head
    val output = Process(s"""scala ${modelJar} ni""", new File(gen_dir)).!!
    val result = output.split("\n").filter(_.contains("Total Cycles for App")).headOption
    if (result.isDefined) {
      totalCycles = "^.*: ".r.replaceAllIn(result.get,"").trim.toInt
      println(s"DSE Model result: $totalCycles")
    }

    super.preprocess(b)
  }

  override protected def postprocess[A](b: Block[A]): Block[A] = {
    super.postprocess(b)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {  }



}
