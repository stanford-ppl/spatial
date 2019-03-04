package models

import java.io.File
import java.io.PrintWriter
import utils.io.files._
import utils.math.{CombinationTree, ReduceTree}

import scala.io.Source

object CongestionModel {

	abstract class FeatureVec[T] {
		def loads: T
		def stores: T
		def gateds: T
		def outerIters: T
		def innerIters: T
		def toSeq: Seq[T] = Seq(stores, outerIters, loads, innerIters, gateds)
	}
	case class RawFeatureVec(loads: Double, stores: Double, gateds: Double, outerIters: Double, innerIters: Double) extends FeatureVec[Double]
	case class CalibFeatureVec(loads: Double, stores: Double, gateds: Double, outerIters: Double, innerIters: Double) extends FeatureVec[Double]

	// Set up lattice properties
	val feature_dims = 5
	val lattice_rank = 5
	val lattice_size = Seq(3,3,3,3,3)
	val num_keypoints = 8
	val num_lattices = 1
	var model: String = ""

	// Derive lattice properties
	val sizes = scala.Array.tabulate(lattice_rank){i => lattice_size(i)}
	val dimensions = sizes.length
	val params_per_lattice = sizes.product
	val strides: scala.Array[Int] = scala.Array.fill(dimensions){1}
	val nparams = num_lattices * params_per_lattice

	// Grab lattice params
	lazy val loads_keypoints_inputs = ModelData.loads_keypoints_inputs(model).map(_.toDouble) //loadCSVNow[Int](s"../data/${model}/CALIBRATOR_INPUT_PARAMS/loads_keypoints_inputs.csv", ","){x => x.toDouble}
	lazy val loads_keypoints_outputs = ModelData.loads_keypoints_outputs(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/CALIBRATOR_OUTPUT_PARAMS/loads_keypoints_outputs.csv", ","){x => x.toDouble}
	lazy val stores_keypoints_inputs = ModelData.stores_keypoints_inputs(model).map(_.toDouble) //loadCSVNow[Int](s"../data/${model}/CALIBRATOR_INPUT_PARAMS/stores_keypoints_inputs.csv", ","){x => x.toDouble}
	lazy val stores_keypoints_outputs = ModelData.stores_keypoints_outputs(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/CALIBRATOR_OUTPUT_PARAMS/stores_keypoints_outputs.csv", ","){x => x.toDouble}
	lazy val gateds_keypoints_inputs = ModelData.gateds_keypoints_inputs(model).map(_.toDouble) //loadCSVNow[Int](s"../data/${model}/CALIBRATOR_INPUT_PARAMS/gateds_keypoints_inputs.csv", ","){x => x.toDouble}
	lazy val gateds_keypoints_outputs = ModelData.gateds_keypoints_outputs(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/CALIBRATOR_OUTPUT_PARAMS/gateds_keypoints_outputs.csv", ","){x => x.toDouble}
	lazy val outerIters_keypoints_inputs = ModelData.outerIters_keypoints_inputs(model).map(_.toDouble) //loadCSVNow[Int](s"../data/${model}/CALIBRATOR_INPUT_PARAMS/outerIters_keypoints_inputs.csv", ","){x => x.toDouble}
	lazy val outerIters_keypoints_outputs = ModelData.outerIters_keypoints_outputs(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/CALIBRATOR_OUTPUT_PARAMS/outerIters_keypoints_outputs.csv", ","){x => x.toDouble}
	lazy val innerIters_keypoints_inputs = ModelData.innerIters_keypoints_inputs(model).map(_.toDouble) //loadCSVNow[Int](s"../data/${model}/CALIBRATOR_INPUT_PARAMS/innerIters_keypoints_inputs.csv", ","){x => x.toDouble}
	lazy val innerIters_keypoints_outputs = ModelData.innerIters_keypoints_outputs(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/CALIBRATOR_OUTPUT_PARAMS/innerIters_keypoints_outputs.csv", ","){x => x.toDouble}
    lazy val params = ModelData.params(model).map(_.toDouble) //loadCSVNow[Double](s"../data/${model}/LATTICE_PARAMS.csv", ","){x => x.toDouble}

    /** Calibrate one element in a feature */
    def calibrate(inputs: Seq[Double], outputs: Seq[Double], feature: Double, max_dim: Int): Double = {
	    val pwl = if (inputs.nonEmpty) {
	      if (feature < inputs.head) outputs.head
	      else if (feature >= inputs.last) outputs.last
	      else (0 until inputs.size-1).collect{case i if (inputs(i) <= feature && feature < inputs(i+1)) => 
	      	if (feature == inputs(i) || outputs(i) == outputs(i+1)) outputs(i)
	      	else outputs(i) + (feature - inputs(i)) * ((outputs(i+1) - outputs(i)) / (inputs(i+1) - inputs(i)))
	      }.headOption.getOrElse(outputs.last)
	    }
	    else 0.0
	    (0.0 max pwl) min {max_dim-1}.toDouble

    }

    /** Run raw features through calibrators */
    def calibrate_features(features: RawFeatureVec): CalibFeatureVec = {
    	val loads = calibrate(loads_keypoints_inputs, loads_keypoints_outputs, features.loads, lattice_size(0))
    	val stores = calibrate(stores_keypoints_inputs, stores_keypoints_outputs, features.stores, lattice_size(1))
    	val gateds = calibrate(gateds_keypoints_inputs, gateds_keypoints_outputs, features.gateds, lattice_size(2))
    	val outerIters = calibrate(outerIters_keypoints_inputs, outerIters_keypoints_outputs, features.outerIters, lattice_size(3))
    	val innerIters = calibrate(innerIters_keypoints_inputs, innerIters_keypoints_outputs, features.innerIters, lattice_size(4))
    	val calib = CalibFeatureVec(loads = loads, stores = stores, gateds = gateds, outerIters = outerIters, innerIters = innerIters)
    	calib
    }

    /** Get all corners in the hypercube */
	def allCorners(maxes: Seq[scala.Int], partials: Seq[Seq[scala.Int]] = Seq(Seq.empty)): Seq[Seq[scala.Int]] = maxes match {
	  case Nil => Nil
	  case h::tail if tail.nonEmpty => (0 to h).flatMap{i => allCorners(tail, partials.map(_ ++ Seq(i)))}
	  case h::tail if tail.isEmpty => (0 to h).flatMap{i => partials.map(_ ++ Seq(i))}
	}

    /** Run calibrated features through hypercube interp */
    def hypercube_features(features: CalibFeatureVec): Double = {
	    val residualPairs: Seq[Seq[Double]] = Seq.tabulate(dimensions) {i =>
	      val x = features.toSeq(i)
	      Seq(x % 1.0, 1.0-(x%1.0))
	    }

	    // Compute all hypervolumes in binary counting order (000, 001, 010, 011, etc..)
	    val hypervolumes: Seq[Double] = CombinationTree[Double](residualPairs:_*)(_*_)
	    // Compute hypercube origin
	    val base: Seq[Int] = Array.tabulate(dimensions) {x => features.toSeq(x).toInt}
	    // Get all vertices of a hypercube and reverse so that these are opposite the hypervolumes
	    val corners: Seq[Seq[scala.Int]] = allCorners(Seq.fill(dimensions)(1)).reverse

	    // Get flat index for each (corner + origin)
	    val indices: Seq[Int] = corners map { c =>
	      val corner = (base zip c.map(_.toInt)) map {case (a,b) => a + b}
	      corner.zipWithIndex.map { case (cc, i) =>
	        cc * lattice_size.drop(i+1).product
	      } reduce {_+_}
	    }

	    // Get weighted sum
	    val x = hypervolumes.map(_.toDouble).zip(indices).collect{case (hv,i) if hv > 0 => hv * params(i)}.sum
	    x
    }

    /** Evaluate model on features (loads: Int, stores: Int, gateds: Int, outerIters: Int, innerIters: Int) */
	def evaluate(features: RawFeatureVec, typ: Runtime.CtrlSchedule): Int = {
		model = typ.toString

		val calibrated_features = calibrate_features(features)
		val result = hypercube_features(calibrated_features)
		result.toInt
	}
}