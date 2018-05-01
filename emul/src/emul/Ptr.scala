package emul

import scala.language.implicitConversions

case class Ptr[T](var x: T) {
  def set(x2: T): Ptr[T] = { x = x2; this }
  def value: T = x

  private var needsInit: Boolean = true
  def initMem(init: T): Unit = if (needsInit) {
    x = init
    needsInit = false
  }
}
