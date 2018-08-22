package spatial.model

import argon._

/** A fixed latency model. Assumes a FPGA-like fabric. */
case class PrimitiveLatencyModel(IR: State) {
  private implicit val _IR: State = IR



  def latency(node: Sym[_]):

}
