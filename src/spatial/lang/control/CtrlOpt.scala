package spatial.lang.control

import core._
import spatial.data._

case class CtrlOpt(
  name:  Option[String] = None,
  sched: Option[UserSchedule] = None,
  ii:    Option[Int] = None,
) {
  def set(x: Sym[_]): Unit = {
    name.foreach{n => x.name = Some(n) }
    sched.foreach{s => userStyleOf(x) = s }
  }
}
