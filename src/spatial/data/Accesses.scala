package spatial.data

import core._

case class UnusedAccess(flag: Boolean) extends StableData[UnusedAccess]

object isUnusedAccess {
  def apply(access: Sym[_]): Boolean = metadata[UnusedAccess](access).exists(_.flag)
  def update(access: Sym[_], flag: Boolean): Unit = metadata.add(access, UnusedAccess(flag))
}
