package spatial

trait SpatialTestbench extends argon.DSLTestbench {
  override def initConfig(): argon.Config = new SpatialConfig
}
