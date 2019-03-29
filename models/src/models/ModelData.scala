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
    case "DenseLoad" => Seq(1.6549126905859908,1.6551518985065565,9.246255986824002e-31,0.0009303487781182237,0.001596320321086487,4.252535209443854,0.1536782578386386,3.5844773279282027e-34,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
    case "DenseStore" => Seq(1.4674586415040312,1.4680697244545626,1.000889167512558e-33,0.002011882352492224,0.0053049796580028055,0.5149810204995141,2.505617098040575,4.4351904541175875e-34,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
    case "GatedDenseStore" => Seq(2.7888615313392475,2.788858258300343,3.775166331562835e-31,0.9759779398821451,0.0015665567049129863,0.41320054852356586,2.3468738486757186,2.5639093034251417,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
  }
}
