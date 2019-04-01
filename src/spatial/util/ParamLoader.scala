package spatial.util

import argon._
import spatial.lang._
import spatial.Spatial
import spatial.metadata.params._
import com.typesafe.config._
import scala.reflect.ClassTag

import scala.collection.mutable
import scala.collection.JavaConverters._

trait ParamLoader { self:Spatial =>

  val params:mutable.Map[String, I32] = mutable.Map.empty

  /*
   * Load parameter from a file defined with command line --load-param=<path>
   * @param name Parameter name
   * @param default Default parameter if file not exists or --load-param not set
   * */
  def loadParam(name:String, default:Int32):I32 = {
    params.getOrElseUpdate(name, default)
  }

  /*
   * Load parameters from a file path 
   * @param path: path to load params from
   * */
  def loadParams(paramPath:String) = {
    val path::field::_ = if (paramPath.contains(":")) {
      paramPath.split(":").map(_.trim).toList
    } else {
      List(paramPath, "params")
    }
    val file = new java.io.File(path)
    if (file.exists & !file.isDirectory) {
      info(s"Loading params from $path")
      val conf = ConfigFactory.load(ConfigFactory.parseFile(file))
      conf.getConfig(field).entrySet.asScala.map { e => 
        val name = e.getKey
        val param = parseParam(e.getValue.unwrapped)
        param.name = Some(name)
        params += name -> param
      }
    } else {
      info(s"Param file $path not exists. Using default params")
    }
  }

  private val fullRange = raw"(\d+)\s*\(\s*(\d+)\s*->\s*(\d+)\s*->\s*(\d)+\s*\)".r
  private val minMax = raw"(\d+)\s*\(\s*(\d+)\s*->\s*(\d+)\s*\)".r
  private val int = raw"(\d+)".r
  private val explicit = raw"(\d+)\s*\(([0-9,]+)\)".r
  private def parseParam(x:Any):I32 = x match {
    case x:Int => I32.p(x)
    case x:String =>
      x match {
        case fullRange(v,min,step,max) => createParam(v.toInt, min.toInt, step.toInt, max.toInt)
        case minMax(v, min, max) => createParam(v.toInt, min.toInt, 1, max.toInt)
        case int(v) => v.toInt.to[I32]
        case explicit(v, possible) => createParam(v.toInt, possible.split(",").map(_.trim.toInt))
        case s => throw new Exception(s"Unexpected string format $s for param of $name")
      }
    case s => throw new Exception(s"Unexpected format $s for param of $name")
  }

  /*
   * Save parameters into a file
   * @param path: File path to store the parameters
   * */
  def saveParams(path:String):Unit = {
    info(s"Saving parameters to $path")
    inGen(path) {
      state.gen.println(s"params {")
      params.foreach { case (name, param) =>
        val c = param.rhs.getValue.get
        val line = param.getParamDomain match {
          case Some(Left((min, step, max))) => s"$name=$c ($min -> $step -> $max)"
          case Some(Right(x)) => s"$name=$c (${x.mkString(",")})"
          case None => s"$name=$c"
        }
        state.gen.println(line)
      }
      state.gen.println(s"}")
    }
  }

}

