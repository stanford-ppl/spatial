package models

import scala.collection.mutable.{HashMap, Set}
import java.nio._
import java.nio.{ file => javafile }
import org.jpmml.evaluator._
import scala.collection.JavaConverters._
import _root_.java.io.File
import org.dmg.pmml.FieldName
import utils.math._
import utils.math.isPow2

class AreaEstimator {
  val openedModels: HashMap[(String,String), Evaluator] = HashMap()
  val failedModels: Set[(String,String)] = Set()

  private var useML: Boolean = true

  implicit def area: AreaEstimator = this

  private def init(useMLFlag: Boolean): Unit = {
    if (useMLFlag) {
      import scala.language.postfixOps
      import sys.process._

      val python3Exists = "python3" !

      val pickleExists = "echo import pickle" #| "python3" !

      val xgboostExists = "echo import xgboost" #| "python3" !
      
      val sklearnExists = "echo import sklearn" #| "python3" !
      
      (python3Exists, pickleExists, xgboostExists, sklearnExists) match {
        case (0,0,0,0) => useML = true
        case (1,_,_,_) => println(s"python3 missing!  Cannot run ML area models!"); useML = false
        case (_,_,_,_) => println(s"Missing python3 libraries: ${if (pickleExists == 1) "pickle" else ""} ${if (xgboostExists == 1) "xgboost" else ""} ${if (sklearnExists == 1) "sklearn" else ""} !  Cannot run ML area models!"); useML = false
      }
    } else {
      useML = false
    }
  }

  def startup(useMLFlag: Boolean): Unit = init(useMLFlag)

  def shutdown(wait: Long = 0): Unit = {
    openedModels.clear()
    failedModels.clear()
  }

  // This needs to be defined by the instantiator
  def estimateMem(prop: String, nodetype: String, dims: Seq[Int], bitWidth: Int, depth: Int, B: Seq[Int], N: Seq[Int], alpha: Seq[Int], P: Seq[Int], histRaw: Seq[Int]): Double = {
    if (useML && nodetype != "") {
      // Setup properties vector
      val padsize = nodetype match {case "SRAMNew" => 5; case "RegFileNew" => 2; case "LineBufferNew" => 2; case _ => 1}
      val allDims = dims.padTo(padsize,0)
      val allB = B.padTo(padsize,0)
      val allN = N.padTo(padsize,0)
      val allAlpha = alpha.padTo(padsize,0)
      val allP = P.padTo(padsize,0)
      val hist = if (histRaw.size > 9) {
        println(s"WARNING: read/write muxWidth histogram ($histRaw) is larger than what is supported by modeling.  Taking last 3 width sizes!")
        histRaw.takeRight(9)
      } else histRaw
      if (hist.grouped(3).exists{x => x(0) == 0 && (x(1) + x(2) > 0)}) println(s"WARNING: histogram ($hist) contains entries connected to 0 banks, which is likely wrong!")
      val values = allB ++ allN ++ allAlpha ++ List(bitWidth) ++ allDims ++ hist ++ List(depth) ++ allP

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
    } else {
      // Set up fall-back penalty model 
      val mulCost = 6
      val divCost = 20
      val modCost = 20
      val muxCost = 6
      val volumePenalty = 1

      if (nodetype != "") {
        // Get powerOf2 composition
        val padding          = dims.zip(P).map{case(d,p) => (p - d%p) % p}
        val w                = dims.zip(padding).map{case (a:Int, b:Int) => a + b}
        val D                = w.size
        val numBanks         = N.product
        val Ns_not_pow2      = N.map{x => if (isPow2(x)) 0 else 1}.sum
        val alphas_not_pow2  = alpha.map{x => if (isPow2(x)) 0 else 1}.sum
        val Ps               = P
        val Pss_not_pow2     = Ps.map{x => if (isPow2(x)) 0 else 1}.sum
        val dimMultipliers   = Seq.tabulate(D){t => (w.slice(t+1,D).zip(Ps.slice(t+1,D)).map{case (x,y) => math.ceil(x/y).toInt}.product)}
        val mults_not_pow2   = dimMultipliers.map{x => if (isPow2(x)) 0 else 1}.sum

        // Partition based on direct/xbar banking
        val directW = histRaw.grouped(3).filter(_.head == 1).map(_.last).sum
        val xbarW = histRaw.grouped(3).filter(_.head > 1).map(_.last).sum
        val directR = histRaw.grouped(3).filter(_.head == 1).map(_.drop(1).head).sum
        val xbarR = histRaw.grouped(3).filter(_.head > 1).map(_.drop(1).head).sum
        val direct = directW + directR
        val xbar = xbarW + xbarR

        // Compute penalty from offset calculation
        val ofsDivPenalty = (direct + xbar) * Pss_not_pow2 * divCost //spatialConfig.target.latencyModel.model("FixDiv")("b" -> 32))
        val ofsMulPenalty = (direct + xbar) * mults_not_pow2 * mulCost //spatialConfig.target.latencyModel.model("FixMul")("b" -> 32))

        // Compute penalty from bank calculation
        val bankMulPenalty = xbar * alphas_not_pow2 * mulCost //spatialConfig.target.latencyModel.model("FixMul")("b" -> 32))
        val bankModPenalty = xbar * Ns_not_pow2 * modCost //spatialConfig.target.latencyModel.model("FixMod")("b" -> 32))

        // Compute penalty from muxes for bank resolution
        val wmuxPenalty = if ((nodetype == "SRAMNew" || nodetype == "LineBufferNew") && xbarW > 0) depth * muxCost * numBanks * numBanks * xbarW else 0
        val rmuxPenalty = if ((nodetype == "SRAMNew" || nodetype == "LineBufferNew") && xbarR > 0) depth * muxCost * numBanks * numBanks * xbarR else 0

        // Compute penalty from volume
        val sizePenalty = depth * w.product * volumePenalty
        
        // println(s"BANKING COST FOR $mem UNDER $banking:")
        // println(s"  depth            = ${depth}")
        // println(s"  volume           = ${w.product}")
        // println(s"  numBanks         = ${numBanks}")
        // println(s"    `- # not pow 2 = ${Ns_not_pow2}")
        // println(s"  alphas           = ${banking.map(_.alphas)}")
        // println(s"    `- # not pow 2 = ${alphas_not_pow2}")
        // println(s"  Ps               = ${banking.map(_.Ps)}")
        // println(s"    `- # not pow 2 = ${Pss_not_pow2}")
        // println(s"  dim multipliers  = ${dimMultipliers}")
        // println(s"    `- # not pow 2 = ${mults_not_pow2}")
        // println(s"  Directly banked accesses: ${direct.map(_.access)}")
        // println(s"  XBar banked accesses:     ${xbar.map(_.access)}")
        // println(s"")    
        // println(s"  ofsDivPenalty  = ${ofsDivPenalty}")
        // println(s"  ofsMulPenalty  = ${ofsMulPenalty}")
        // println(s"  bankMulPenalty  = ${bankMulPenalty}")
        // println(s"  bankModPenalty  = ${bankModPenalty}")
        // println(s"  wmuxPenalty  = ${wmuxPenalty}")
        // println(s"  rmuxPenalty  = ${rmuxPenalty}")
        // println(s"  sizePenalty  = ${sizePenalty}")
        // println(s"")

        val totalCost = (ofsDivPenalty + ofsMulPenalty + bankMulPenalty + bankModPenalty + wmuxPenalty + rmuxPenalty + sizePenalty).toLong

        // println(s"TOTAL COST: $totalCost")

        totalCost
      } else {
        val w                = dims
        val sizePenalty    = depth * w.product * volumePenalty

        val numWriters = histRaw.grouped(3).map(_.last).sum

        // Assume direct banking for W, and crossbar for readers
        val rmuxPenalty = if (nodetype == "SRAMNew" || nodetype == "LineBufferNew") depth * muxCost * numWriters * numWriters else 0

        // dbg(s"BANKING COST FOR $mem UNDER DUPLICATION:")
        // dbg(s"  depth            = ${depth}")
        // dbg(s"  volume           = ${w.product}")
        // println(s"  rmuxPenalty  = ${rmuxPenalty}")
        // println(s"")

        val totalCost =  histRaw.grouped(3).map(_.drop(1).head).sum * (sizePenalty + rmuxPenalty).toLong

        // println(s"TOTAL COST: $sizePenalty")

        totalCost
      }
    }
  }

  def estimateArithmetic(prop: String, nodetype: String, values: Seq[Int]): Double = {
    if (useML) {
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
    } else {
      0.0
    }
  }
}