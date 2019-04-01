package models

object ModelData{
  def params(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.params
    case "DenseStore" => DenseStoreModelData.params
    case "GatedDenseStore" => GatedDenseStoreModelData.params
  }
  def innerIters_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.innerIters_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.innerIters_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.innerIters_keypoints_inputs
  }
  def stores_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.stores_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.stores_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.stores_keypoints_inputs
  }
  def gateds_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.gateds_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.gateds_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.gateds_keypoints_inputs
  }
  def outerIters_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.outerIters_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.outerIters_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.outerIters_keypoints_inputs
  }
  def bitsPerCycle_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.bitsPerCycle_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.bitsPerCycle_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.bitsPerCycle_keypoints_inputs
  }
  def loads_keypoints_inputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.loads_keypoints_inputs
    case "DenseStore" => DenseStoreModelData.loads_keypoints_inputs
    case "GatedDenseStore" => GatedDenseStoreModelData.loads_keypoints_inputs
  }
  def outerIters_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.outerIters_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.outerIters_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.outerIters_keypoints_outputs
  }
  def gateds_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.gateds_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.gateds_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.gateds_keypoints_outputs
  }
  def stores_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.stores_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.stores_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.stores_keypoints_outputs
  }
  def innerIters_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.innerIters_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.innerIters_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.innerIters_keypoints_outputs
  }
  def bitsPerCycle_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.bitsPerCycle_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.bitsPerCycle_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.bitsPerCycle_keypoints_outputs
  }
  def loads_keypoints_outputs(schedule: String) = schedule match {
    case "DenseLoad" => DenseLoadModelData.loads_keypoints_outputs
    case "DenseStore" => DenseStoreModelData.loads_keypoints_outputs
    case "GatedDenseStore" => GatedDenseStoreModelData.loads_keypoints_outputs
  }
  def curve_fit(schedule: String): Seq[Double] = schedule match {
    case "DenseLoad" => Seq(2.154845938002914,0.03197213083608923,1.383120208551509e-06,0.007284867561777253,0.02357769292060957,9.151851837194858,5.022555284050247e-06,4.884871781413413e-12,31.604812429176626,1.0,1.0,1.0,1.0,1.0,0.026981677721912196)
    case "DenseStore" => Seq(5.017302179248052,0.08249901105867254,1.6578229102411373,44.99286597371387,0.011258890176311211,1.1390317830807934,5.8444331658716635,1.2198247648829588e-33,2.829346834134116,1.0,1.0,1.0,1.0,1.0,0.007678689823914727)
    case "GatedDenseStore" => Seq(5.919985193058772,0.11885797442183878,0.7775669911478581,206.84857692201862,0.010146011043424682,0.7004795974185706,3.7864489098445917,4.347691187618166,4.838594748866312,1.0,1.0,1.0,1.0,1.0,0.006001312012503769)
}
}
