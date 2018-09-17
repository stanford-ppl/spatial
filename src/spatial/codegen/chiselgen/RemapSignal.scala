package spatial.codegen.chiselgen

sealed trait RemapSignal
// "Standard" Signals
object En extends RemapSignal
object Done extends RemapSignal
object BaseEn extends RemapSignal
object Mask extends RemapSignal
object Resetter extends RemapSignal
object DatapathEn extends RemapSignal
object CtrTrivial extends RemapSignal
// A few non-canonical signals
object IIDone extends RemapSignal
object RstEn extends RemapSignal
object CtrEn extends RemapSignal
object Ready extends RemapSignal
object Valid extends RemapSignal
object NowValid extends RemapSignal
object Inhibitor extends RemapSignal
object Wren extends RemapSignal
object Chain extends RemapSignal
object Blank extends RemapSignal
object DataOptions extends RemapSignal
object ValidOptions extends RemapSignal
object ReadyOptions extends RemapSignal
object EnOptions extends RemapSignal
object RVec extends RemapSignal
object WVec extends RemapSignal
object Latency extends RemapSignal
object II extends RemapSignal
object SM extends RemapSignal
object Inhibit extends RemapSignal
object Flow extends RemapSignal