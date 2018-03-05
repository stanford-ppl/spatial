package emul

class Bool(val value: Boolean, val valid: Boolean) extends Equals {
  def &&(that: Bool)  = Bool(this.value && that.value, this.valid && that.valid)
  def ||(that: Bool)  = Bool(this.value || that.value, this.valid && that.valid)
  def ^(that: Bool)   = Bool(this.value ^ that.value, this.valid && that.valid)
  def !==(that: Bool) = Bool(this.value != that.value, this.valid && that.valid)
  def ===(that: Bool) = Bool(this.value == that.value, this.valid && that.valid)

  def toBoolean: Boolean = value

  override def toString: String = if (valid) { value.toString } else "X"
  def toStr: String = if (valid && value) "1" else if (valid) "0" else "X"

  override def hashCode(): Int = (value,valid).hashCode()

  override def canEqual(that: Any): Boolean = that match {
    case _: Bool => true
    case _: Boolean => true
    case _ => false
  }

  override def equals(o: Any): Boolean = o match {
    case that: Bool => this.value == that.value && this.valid == that.valid
    case that: Boolean => this.value == that
    case _ => false
  }
}
object FALSE extends Bool(false, true)
object TRUE  extends Bool(true, true)

object Bool {
  def apply(value: Boolean): Bool = new Bool(value, valid=true)
  def apply(value: Boolean, valid: Boolean): Bool = new Bool(value, valid)
}
