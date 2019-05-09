package models

import scala.collection.mutable.{HashMap, Set}
import java.nio._
import java.nio.{ file => javafile }
import org.jpmml.evaluator._
import scala.collection.JavaConverters._
import _root_.java.io.File
import org.dmg.pmml.FieldName

class AreaEstimator {
  val openedModels: HashMap[(String,String), Evaluator] = HashMap()
  val failedModels: Set[(String,String)] = Set()

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
    val path = "/home/tianzhao/area_analyzer/pmmls_bak/"
    val modelName = nodetype + "_" + prop + ".pmml"
    val model_exists = javafile.Files.exists(javafile.Paths.get(path + modelName))
    if (model_exists) {
      val evaluator: Evaluator = openedModels.getOrElseUpdate((nodetype,prop), {
        println(s"Loading area model for $prop of $nodetype")
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
      val resultExtractor = raw".*result=(-?\d+)\.(\d+).*".r
      val resultExtractor(dec,frac) = targetFields
      val result = (dec + "." + frac).toDouble
      result
    } else {
      if (!failedModels.contains((nodetype,prop))) println(s"WARNING: No model for $prop of $nodetype node (${path + modelName})!")
      failedModels += ((nodetype,prop))
      0.0
    }
  }
}
