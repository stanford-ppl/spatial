package spatial.data

import argon._

case class Global(flag: Boolean) extends FlowData[Global]

object isGlobal {
  def apply(e: Sym[_]): Boolean = e.isValue || metadata[Global](e).exists(_.flag)
  def update(e: Sym[_], flag: Boolean): Unit = metadata.add(e, Global(flag))
}
