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


case class LatencyAnalyzer(IR: State, latencyModel: LatencyModel) extends AccelTraversal {
  var cycleScope: List[Double] = Nil
  var intervalScope: List[Double] = Nil
  var totalCycles: Seq[Long] = Seq()

  def getListOfFiles(d: String):List[String] = {
    import java.nio.file.{FileSystems, Files}
    import scala.collection.JavaConverters._
    val dir = FileSystems.getDefault.getPath(d) 
    Files.walk(dir).iterator().asScala.filter(Files.isRegularFile(_)).map(_.toString).toList//.foreach(println)
  }
  
  override def silence(): Unit = {
    super.silence()
  }


  def test(rewriteParams: Seq[Seq[Any]]): Unit = {
    import scala.language.postfixOps
    import java.io.File
    import sys.process._

    val gen_dir = if (config.genDir.startsWith("/")) config.genDir + "/" else config.cwd + s"/${config.genDir}/"
    val modelJar = getListOfFiles(gen_dir + "/model").filter(_.contains("RuntimeModel-assembly")).head
    totalCycles = rewriteParams.grouped(200).flatMap{params => 
      val batchedParams = params.map{rp => "tune " + rp.mkString(" ")}.mkString(" ")
      val cmd = s"""java -jar ${modelJar} ni ${batchedParams}"""
      println(s"running cmd: $cmd")
      val output = Process(cmd, new File(gen_dir)).!!
      output.split("\n").filter(_.contains("Total Cycles for App")).map{r => 
        "^.*: ".r.replaceAllIn(r,"").trim.toLong
      }.toSeq
    }.toSeq
    // println(s"DSE Model result: $totalCycles")

  }

  override protected def preprocess[A](b: Block[A]): Block[A] = {

    super.preprocess(b)
  }

  override protected def postprocess[A](b: Block[A]): Block[A] = {
    super.postprocess(b)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {  }



}
