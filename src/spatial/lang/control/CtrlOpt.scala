package spatial.lang.control

import argon._
import spatial.data._

case class CtrlOpt(
  name:  Option[String] = None,
  sched: Option[Sched] = None,
  ii:    Option[Int] = None,
) {
  def set[A](x: Sym[A]): Unit = {
    name.foreach{n => x.name = Some(n) }
    sched.foreach{s => userStyleOf(x) = s }
  }
}
