package emul
import scala.reflect.{ClassTag, classTag}

class Memory[T:ClassTag](name: String) {
  var data: Array[T] = _
  var bits: Int = -1
  private var needsInit: Boolean = true
  def initMem(size: Int, zero: T): Unit = if (needsInit) {
    data = Array.fill(size)(zero)
    needsInit = false
    bits = zero match {
      case k: FixedPoint => k.fmt.bits
      case _ => -1
    }
  }

  def apply(i: Int): T = {
    DRAMTracker.accessMap((classTag[T], bits, "read")) += 1
    data.apply(i)
  }
  def update(i: Int, x: T): Unit = {
    DRAMTracker.accessMap((classTag[T], bits, "write")) += 1
    data.update(i, x)
  }
}
