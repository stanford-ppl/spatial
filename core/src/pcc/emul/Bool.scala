package pcc.emul

class Bool(val value: Boolean, val valid: Boolean) {
  def &&(that: Bool)  = Bool(this.value && that.value, this.valid && that.valid)
  def ||(that: Bool)  = Bool(this.value || that.value, this.valid && that.valid)
  def ^(that: Bool)   = Bool(this.value ^ that.value, this.valid && that.valid)
  def !==(that: Bool) = Bool(this.value != that.value, this.valid && that.valid)
  def ===(that: Bool) = Bool(this.value == that.value, this.valid && that.valid)

  override def toString: String = if (valid) { value.toString } else "X"
  def toStr: String = if (valid && value) "1" else if (valid) "0" else "X"
}
object FALSE extends Bool(false, true)
object TRUE  extends Bool(true, true)

object Bool {
  def apply(value: Boolean): Bool = new Bool(value, valid=true)
  def apply(value: Boolean, valid: Boolean): Bool = new Bool(value, valid)
}
