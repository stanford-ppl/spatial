package argon.transform

trait TransformerInterface {
  def apply[T](x: T): T
}
