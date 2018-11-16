package spatial
package tests
package plasticine

import spatial.util.spatialConfig

trait PlasticineTest extends SpatialTest {
  override def backends: Seq[Backend] = super.backends :+ PIR 

  object PIR extends PIRBackEnd (
    name="PIR"
  )(
    args = "--pir --dot --vv",
    run = s"bash run.sh --dot=false --run-psim --net=asic --trace=false --mapping=false"
  )
}
