package spatial.util

import argon._
import spatial.lang._
import spatial.Spatial
import spatial.metadata.params._
import com.typesafe.config.ConfigFactory
import scala.reflect.ClassTag
import scala.util.matching.Regex

trait ParamLoader { self:Spatial =>
  implicit class Parameters(conf:com.typesafe.config.Config) {
    val fullRange = """.+\s*=\s*\d+\(\s*\d+\s*->\s*\d+\s*->\s*\d+\s*\)""".r
    val minMax = """.+\s*=\s*\d+\(\s*\d+\s*->\s*\d+\s*\)""".r
    val int = """.+\s*=\s*\d+""".r
    def apply(name:String):I32 = {
      conf.getString(name) match {
        case fullRange(v,min,step,max) => createParam(v.toInt, min.toInt, step.toInt, max.toInt)
        case minMax(v, min, max) => createParam(v.toInt, min.toInt, 1, max.toInt)
        case int(v) => v.toInt.to[I32]
      }
    }
  }

  def loadParams(defaults:(String, I32)*) = {
    val confStr = defaults.map { case (name, param) =>
      param.getParamDomain match {
        case Some((min, step, max)) => s"$name=$param ($min -> $step -> $max)"
        case None => s"$name=$param"
      }
    }.mkString("\n")
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

}
