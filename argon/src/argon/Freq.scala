package argon

object Freq extends Enumeration {
  type Freq = Value
  val Cold, Normal, Hot = Value

  def combine(a: Freq, b: Freq): Freq = (a,b) match {
    case (Cold, _) => Cold
    case (_, Cold) => Cold
    case (Hot, _)  => Hot
    case (_, Hot)  => Hot
    case _         => Normal
  }
}