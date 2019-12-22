package spatial.metadata.blackbox

import argon._


case class BlackboxConfig(file: String, moduleName: Option[String] = None, latency: scala.Int = 1, pf: scala.Int = 1, params: Map[String, Any] = Map())

/** Information needed to manage a verilog black box.
  * Post-unrolling:
  * Option:  sym.getBboxInfo
  * Getter:  sym.bboxInfo
  * Setter:  sym.bboxInfo = (file, latency, pipelined, paramsMap))
  * Default: undefined
  */
case class BlackboxInfo(cfg: BlackboxConfig) extends Data[BlackboxInfo](SetBy.User)

case class BlackboxUserNodes(node: Seq[Sym[_]]) extends Data[BlackboxUserNodes](SetBy.Flow.Consumer)
