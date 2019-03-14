package spatial.targets

import java.io.File

import argon._
// import org.encog.engine.network.activation.ActivationSigmoid
// import org.encog.ml.data.basic.{BasicMLData, BasicMLDataSet}
// import org.encog.neural.networks.BasicNetwork
// import org.encog.neural.networks.layers.BasicLayer
// import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation
// import org.encog.persist.EncogDirectoryPersistence

import scala.collection.JavaConverters._
import scala.io.Source

class TileLoadModel {
  // val name = "TileLoadModel"
  // val filename = "TileLoadData.csv" // TODO: Shouldn't be hardcoded

  // private var network: BasicNetwork = _
  // private def needsInit = network eq null
  // val verbose = false
  // val MAX_EPOCH = 600

  // private val cwd = System.getenv().getOrDefault("SPATIAL_HOME", new java.io.File(".").getAbsolutePath)

  def init(): Unit = {
    // if (needsInit) {
    //   val encogFile = s"$cwd/data/$name.eg"
    //   val exists = new File(encogFile).exists

    //   if (exists) {
    //     dbgs("Loaded " + name + " model from file")
    //     network = EncogDirectoryPersistence.loadObject(new File(encogFile)).asInstanceOf[BasicNetwork]
    //   }
    //   else {
    //     val MODELS = 1000

    //     val data = Source.fromFile(s"$cwd/data/$filename").getLines().toArray.drop(1).map(_.split(",").map(_.trim.toDouble))

    //     val input = data.map(_.take(4))

    //     val output = data.map(_.slice(4,5))
    //     if (verbose) dbgs(output.map(_.mkString(", ")).mkString(", "))
    //     val trainingSet = new BasicMLDataSet(input, output)
    //     var iter = 0
    //     var minError = Double.PositiveInfinity
    //     var maxError = Double.PositiveInfinity
    //     while (iter < MODELS) {
    //       val (curNetwork, curError, curMax) = trainOne(trainingSet)
    //       if (curMax < maxError) {
    //         minError = curError
    //         maxError = curMax
    //         network = curNetwork
    //       }
    //       iter += 1
    //     }
    //     dbgs(name + "\n-----------------")
    //     dbgs("Neural network results:")
    //     dbgs(s"Average error: ${100*minError/trainingSet.size}%")
    //     dbgs(s"Maximum observed error: ${100*maxError}")

    //     EncogDirectoryPersistence.saveObject(new File(encogFile), network)
    //   }
    // }
  }

  // private def trainOne(trainingSet: BasicMLDataSet) = {
  //   val network = new BasicNetwork()
  //   network.addLayer(new BasicLayer(null,true,4))
  //   network.addLayer(new BasicLayer(new ActivationSigmoid(),true,6))
  //   network.addLayer(new BasicLayer(new ActivationSigmoid(),false,1))
  //   network.getStructure.finalizeStructure()
  //   network.reset()

  //   var epoch = 1
  //   val train = new ResilientPropagation(network, trainingSet)
  //   train.iteration()
  //   while (epoch < MAX_EPOCH) {
  //     //dbgs(s"Epoch #$epoch Error: ${100*train.getError()}")
  //     epoch += 1
  //     train.iteration()
  //   }
  //   train.finishTraining()
  //   //
  //   //dbgs(s"Completed training at epoch $epoch with error of ${100*train.getError()}")

  //   var errors = 0.0
  //   var maxError = 0.0
  //   for (pair <- trainingSet.asScala) {
  //     val output = network.compute(pair.getInput)
  //     val diff = output.getData(0) - pair.getIdeal.getData(0)
  //     val error = diff / pair.getIdeal.getData(0)

  //     if (Math.abs(error) > maxError) maxError = Math.abs(error)
  //     errors += Math.abs(error)
  //   }

  //   (network, errors, maxError)
  // }

  def evaluate(c: Int, r: Int, b: Int, p: Int): Double = {
    // if (needsInit) init()
    // val input = Array(c.toDouble/15, r.toDouble/10000, b.toDouble/(96*256), p.toDouble/192)
    // val output = network.compute(new BasicMLData(input))
    // output.getData(0)
    0.0
  }
}
