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
    case "DenseLoad" => Seq(1.663321289678496,1.6638945217897327,1.0428820695470546e-31,0.001331115586098151,0.0015791015995697086,4.254827736002197,0.1536831110113278,3.6665223048458167e-33,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
    case "DenseStore" => Seq(1.7011173568050562,1.7011607818943721,2.706426560369223e-35,0.21217593655643857,0.00352921572059811,0.5764186232738144,2.8037622194040024,2.636927814874262e-38,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
    case "GatedDenseStore" => Seq(1.1306087960501676,1.1306208639559738,3.5437690893102444e-30,0.3630138682099245,0.01491455077018495,0.2642968572667707,1.49972839858081,1.6385370212250072,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
}
}
