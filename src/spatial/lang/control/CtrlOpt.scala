package spatial.lang.control

import argon._
import spatial.metadata.control._
import spatial.node._
import spatial.lang._

case class CtrlOpt(
  name:  Option[String] = None,
  sched: Option[CtrlSchedule] = None,
  ii:    Option[Int] = None,
  stopWhen: Option[Reg[Bit]] = None,
) {
  def set[A](x: Sym[A]): Unit = {
    name.foreach{n => x.name = Some(n) }
    sched.foreach{s => x.userSchedule = s }
    x.userII = ii.map(_.toDouble)
  }
}
