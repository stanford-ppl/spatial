package spatial.lang

case class Pin(name: String) {
  override def toString: String = name
}

abstract class Bus {
  def length: Int
}

object Bus {
  def apply(valid: Pin, data: Pin*) = PinBus(valid, data)
  def apply(valid: String, data: String*) = PinBus(Pin(valid), data.map(Pin.apply))
}

case class PinBus(valid: Pin, data: Seq[Pin]) extends Bus {
  override def toString: String = "Bus(" + valid.toString + ": " + data.mkString(", ") + ")"
  def length: Int = data.length
}
