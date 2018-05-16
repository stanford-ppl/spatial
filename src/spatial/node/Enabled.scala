package spatial.node

import argon._
import spatial.lang._

trait Enabled[R] { this: Op[R] =>
  var ens: Set[Bit]

  def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    val saveEns = ens
    ens ++= addEns
    val op2 = this.mirror(f)
    ens = saveEns
    op2
  }

  def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    ens ++= addEns
    this.update(f)
  }
}
