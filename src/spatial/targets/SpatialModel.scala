package spatial.targets

import argon._
import models._
import forge.tags._
import spatial.data.Expect
import spatial.internal.spatialConfig

import scala.io.Source
import scala.util.Try

abstract class SpatialModel[F[A]<:Fields[A,F]](target: HardwareTarget) extends NodeParams {
  final type ResModel  = Model[NodeModel,F]
  final type Resources = Model[Double,F]
  final def DSP_CUTOFF: Int = target.DSP_CUTOFF

  val FILE_NAME: String
  val RESOURCE_NAME: String
  def FIELDS: Array[String]
  implicit def RESOURCE_FIELDS: F[Double]
  implicit def MODEL_FIELDS: F[NodeModel]
  def NONE: Resources = Model.zero[Double,F]
  def NO_MODEL: ResModel = Model.zero[NodeModel,F]

  private var missing: Set[String] = Set[String]()
  private var needsInit: Boolean = true
  var recordMissing: Boolean = true

  def silence(): Unit = { recordMissing = false }
  def reset(): Unit = { missing = Set.empty }

  @stateful def init(): Unit = if (needsInit) {
    models = loadModels()
    needsInit = false
  }

  @inline def miss(str: String): Unit = if (recordMissing) { missing += str }

  var models: Map[String,ResModel] = Map.empty
  def model(name: String)(args: (String,Double)*): Resources = {
    models.get(name).map{model => model.eval(args:_*) }.getOrElse{
      val params = if (args.isEmpty) "" else " [optional parameters: " + args.map(_._1).mkString(", ") + "]"
      miss(s"$name (csv)" + params)
      NONE
    }
  }
  @stateful def model(sym: Sym[_], key: String): Double = model(sym).apply(key)

  @stateful def model(sym: Sym[_]): Resources = sym match {
    case Expect(_) => NONE
    case Op(op) =>
      val (name, params) = nodeParams(sym, op)
      model(name)(params:_*)
    case _ => NONE
  }

  @stateful def reportMissing(): Unit = {
    if (missing.nonEmpty) {
      warn(s"The target device ${target.name} was missing one or more $RESOURCE_NAME models.")
      missing.foreach{str => warn(s"  $str") }
      warn(s"Models marked (csv) can be added to $FILE_NAME.")
      warn("")
      state.logWarning()
    }
  }


  @stateful def loadModels(): Map[String,ResModel] = {
    val resource = Try(Source.fromResource("models/" + FILE_NAME).getLines())
    val direct = Try{
      val SPATIAL_HOME = sys.env("SPATIAL_HOME")
      Source.fromFile(SPATIAL_HOME + "/models/" + FILE_NAME).getLines()
    }
    val file: Option[Iterator[String]] = {
      if (resource.isSuccess) Some(resource.get)
      else if (direct.isSuccess) Some(direct.get)
      else None
    }

    file.map{lines =>
      val headings = lines.next().split(",").map(_.trim)
      val nParams  = headings.lastIndexWhere(_.startsWith("Param")) + 1
      val indices  = headings.zipWithIndex.filter{case (head,i) => FIELDS.contains(head) }.map(_._2)
      val fields   = indices.map{i => headings(i) }
      val missing = FIELDS diff fields
      if (missing.nonEmpty) {
        warn(s"$RESOURCE_NAME model file $FILE_NAME for target ${target.name} was missing expected fields: ")
        warn(missing.mkString(", "))
      }
      val models = lines.flatMap{line =>
        val parts = line.split(",").map(_.trim)
        if (parts.nonEmpty) {
          val name = parts.head
          val params = parts.slice(1, nParams).filterNot(_ == "")
          val entries = indices.map { i => if (i < parts.length) LinearModel.fromString(parts(i)) else Right(0.0) }
          Some(name -> Model.fromArray[NodeModel,F](name, params, entries))
        }
        else None
      }.toMap

      models
    }.getOrElse{
      warn(s"$RESOURCE_NAME model file $FILE_NAME for target ${target.name} was not found.")
      Map.empty
    }
  }




}
