package spatial
package tests
package plasticine

import spatial.util.spatialConfig

trait PlasticineTest extends SpatialTest { test =>
  override def backends: Seq[Backend] = super.backends :+ PIR :+ PIRGen :+ PIRAsicRun :+ PIRRun :+ P2P

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
    run = s"bash run.sh --dot=false --run-psim --net=asic --trace=true --mapping=true --row=10 --col=10"
    //run = s"bash run.sh --dot=false --trace=false --mapping=true"
  )

  object P2P extends PIRBackEnd (
    name="P2P"
  )(
    args = "--pir --dot",
    run = s"bash run.sh --dot=false --run-psim --net=p2p --trace=true --mapping=true --row=10 --col=10"
  )

  object PIRGen extends PIRBackEnd (
    name="PIRGen"
  )(
    args = "--pir",
    run = s"bash run.sh --trace=true --dot=true --mapping=false --codegen=false --psim=false"
  ) {
    override def genDir(name:String) = s"${IR.config.cwd}/gen/PIR/$name/"
    override def logDir(name:String) = s"${IR.config.cwd}/logs/PIR/$name/"
    override def repDir(name:String) = s"${IR.config.cwd}/reports/PIR/$name/"
  }

  object PIRAsicRun extends PIRBackEnd (
    name="PIRAsicRun"
  )(
    args = "--pir",
    run = s"bash run.sh --dot=false --load --run-psim --net=asic --trace=true --mapping=true"
  ) {
    override def genDir(name:String) = s"${IR.config.cwd}/gen/PIR/$name/"
    override def logDir(name:String) = s"${IR.config.cwd}/logs/PIR/$name/"
    override def repDir(name:String) = s"${IR.config.cwd}/reports/PIR/$name/"
  }

  object PIRRun extends PIRBackEnd (
    name="PIRRun"
  )(
    args = "--pir",
    run = s"bash run.sh --dot=false --load --run-psim --net=checkerboard --trace=false --mapping=true --row=10 --col=10"
  ) {
    override def genDir(name:String) = s"${IR.config.cwd}/gen/PIR/$name/"
    override def logDir(name:String) = s"${IR.config.cwd}/logs/PIR/$name/"
    override def repDir(name:String) = s"${IR.config.cwd}/reports/PIR/$name/"
  }

}
