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
}
