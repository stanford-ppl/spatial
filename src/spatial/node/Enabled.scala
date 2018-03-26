package spatial.node

import argon._
import spatial.lang._

trait Enabled[R] { this: Op[R] =>
  var ens: Set[Bit]

  def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    ens ++= addEns
    this.mirror(f)
  }
  def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    ens ++= addEns
    this.update(f)
  }
}
