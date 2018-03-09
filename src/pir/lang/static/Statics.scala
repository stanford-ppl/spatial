package pir.lang.static

trait Statics extends Pointers

/** Internal view of PIR */
trait InternalStatics extends Statics with InternalAliases

/** External view for extending DSLs */
trait ExtensionStatics extends InternalStatics with ExternalAliases

/** Application view */
trait ExternalStatics extends ExtensionStatics {
  type PIRApp = pir.PIRApp
}
