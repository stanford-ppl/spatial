package spatial.util

import argon._
import spatial.lang._
import spatial.Spatial
import spatial.metadata.params._
import com.typesafe.config._
import scala.reflect.ClassTag

trait ParamLoader { self:Spatial =>
  type TConfig = com.typesafe.config.Config
  implicit class Parameters(conf:TConfig) {
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

  /*
   * Load parameter from a file path specified with command line option --param-path=file. If not
   * specified use default parameters
   * @param defaults: default parameter value
   * */
  def loadParams(defaults:(String, I32)*) = {
    var confStr = defaults.map { case (name, param) =>
      val c = param.rhs.getValue.get
      param.getParamDomain match {
        case Some((min, step, max)) => s"$name=$c ($min -> $step -> $max)"
        case None => s"$name=$c"
      }
    }.mkString("\n")
    confStr = s"params{$confStr}"
    val defaultParam = ConfigFactory.parseString(confStr)
    val conf = spatialConfig.paramPath match {
      case None => 
        info("Using default params")
        defaultParam
      case Some(path) => 
        info(s"Loading params from $path")
        ConfigFactory.load(ConfigFactory.parseFile(new java.io.File(path)))
    }
    conf
  }

  /*
   * Save parameters into a file
   * @param param: Parameter to save
   * @param path: File path to store the parameters
   * */
  def saveParams(param:TConfig, path:String):Unit = {
    info(s"Saving parameters to $path")
    val confStr = param.getValue("params").render(ConfigRenderOptions.concise().setJson(false).setFormatted(true))
    inGen(path) {
      state.gen.println(s"params{")
      state.gen.println(s"$confStr")
      state.gen.println(s"}")
    }
  }

}
