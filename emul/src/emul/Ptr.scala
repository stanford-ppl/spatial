package emul

import scala.language.implicitConversions

case class Ptr[T](var x: T) {
  def set(x2: T): Ptr[T] = { x = x2; this }
  def value: T = x

  private var  initValue : T = _
  private var needsInit: Boolean = true
  def initMem(init: T): Unit = if (needsInit) {
    x = init
    initValue = init 
    needsInit = false
  }

  def reset() : Ptr[T] =  { x = initValue; this }
}
