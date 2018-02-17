package nova.core

case class BlockOptions(
  temp: Freq.Freq,
  isol: Boolean,
  seal: Boolean
)
object BlockOptions {
  lazy val Normal = BlockOptions(Freq.Normal, isol = false, seal = false)
  lazy val Sealed = BlockOptions(Freq.Cold,   isol = false, seal = true)
}
