package emul

import scala.language.implicitConversions

case class Ptr[T](var x: T) {
  def set(x2: T): Ptr[T] = { x = x2; this }
  def value: T = x

  private var  initValue : T = _
  def initMem(init: T): Unit = {
    x = init
    initValue = init
  }

  def reset() : Ptr[T] =  { x = initValue; this }
}
