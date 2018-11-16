package spatial
package tests
package plasticine

import spatial.util.spatialConfig

trait PlasticineTest extends SpatialTest { test =>
  override def backends: Seq[Backend] = super.backends :+ PIR :+ PIRGen :+ PIRAsicRun

  trait ExecuteBackend extends Backend {
    override def runBackend() = {
      s"${test.name}" should s"run for backend $name" in {
        val result = 
          runMake() ==>
          runApp()
        result.resolve()
      }
    }
  }

  object PIR extends PIRBackEnd (
    name="PIR"
  )(
    args = "--pir --dot",
    run = s"bash run.sh --dot=false --run-psim --net=asic --trace=false --mapping=false"
  )

  object PIRGen extends PIRBackEnd (
    name="PIRGen"
  )(
    args = "--pir",
    run = s"bash run.sh --dot=true --mapping=false --codegen=false --psim=false"
  )

  object PIRAsicRun extends PIRBackEnd (
    name="PIRAsicRun"
  )(
    args = "--pir",
    run = s"bash run.sh --dot=true --load --run-psim --net=asic --trace=false --mapping=true"
  )

}
