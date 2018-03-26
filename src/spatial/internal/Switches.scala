package spatial.internal

import argon._
import forge.tags._

import spatial.node._
import spatial.lang._

trait Switches {

  @rig def op_case[R:Type](body: Block[R]): R = stage(SwitchCase(body))

  @rig def op_switch[R:Type](selects: Seq[Bit], cases: Seq[() => R]): R = {
    val options = BlockOptions(sched = Some(SwitchScheduler))
    val block = stageScope(Nil,options){ cases.map{c => c() }.last }
    stage(Switch(selects, block))
  }

}
