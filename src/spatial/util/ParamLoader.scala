package spatial.util

import argon._
import spatial.lang._
import spatial.Spatial
import spatial.metadata.params._
import com.typesafe.config._
import scala.reflect.ClassTag

trait ParamLoader { self:Spatial =>

  private var _params:Option[Parameters] = None
  def params:Parameters = _params.getOrElse {
    throw new Exception(s"No params intialized! Set it with defaultParams or load it from file with --param-path=<path>")
  } 

  /*
   * @param defaults default parameters
   * */
  def defaultParams(defaults:(String, I32)*) = {
    var confStr = defaults.map { case (name, param) =>
      val c = param.rhs.getValue.get
      param.getParamDomain match {
        case Some((min, step, max)) => s"$name=$c ($min -> $step -> $max)"
        case None => s"$name=$c"
      }
    }.mkString("\n")
    confStr = s"params{$confStr}"
    _params = Some(Parameters(ConfigFactory.parseString(confStr)))
  }

  /*
   * Load parameters from a file path 
   * @param path: path to load params from
   * */
  def loadParams(path:String) = {
    info(s"Loading params from $path")
    val conf = ConfigFactory.load(ConfigFactory.parseFile(new java.io.File(path)))
    _params = Some(Parameters(conf))
  }

  /*
   * Save parameters into a file
   * @param param: Parameter to save
   * @param path: File path to store the parameters
   * */
  def saveParams(param:Parameters, path:String):Unit = {
    info(s"Saving parameters to $path")
    val confStr = param.conf.getValue("params").render(ConfigRenderOptions.concise().setJson(false).setFormatted(true))
    inGen(path) {
      state.gen.println(s"params{")
      state.gen.println(s"$confStr")
      state.gen.println(s"}")
    }
  }

  /*
   * Save params into a file
   * @param path: File path to store the parameters
   * */
  def saveParams(path:String):Unit = {
    saveParams(params, path)
  }

}

case class Parameters(conf:com.typesafe.config.Config)(implicit state:State) {
  val fullRange = raw"(\d+)\s*\(\s*(\d+)\s*->\s*(\d+)\s*->\s*(\d)+\s*\)".r
  val minMax = raw"(\d+)\s*\(\s*(\d+)\s*->\s*(\d+)\s*\)".r
  val int = raw"(\d+)".r
  def apply(name:String):I32 = {
    conf.getString(s"params.$name").trim match {
      case fullRange(v,min,step,max) => createParam(v.toInt, min.toInt, step.toInt, max.toInt)
      case minMax(v, min, max) => createParam(v.toInt, min.toInt, 1, max.toInt)
      case int(v) => v.toInt.to[I32]
      case s => throw new Exception(s"Unexpected format $s for param=$name")
    }
  }
}

