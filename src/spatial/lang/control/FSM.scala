package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._

object FSM {
  @api def apply[A:Bits](init: Lift[A])(notDone: A => Bit)(action: A => Void)(next: A => A): Void = {
    fsm(init.unbox, notDone, action, next)
  }
  @api def apply[A:Bits](notDone: A => Bit)(action: A => Void)(next: A => A): Void = {
    fsm(implicitly[Bits[A]].zero, notDone, action, next)
  }

  @rig def fsm[A:Bits](start: A, notDone: A => Bit, action: A => Void, nextState: A => A): Void = {
    val cur = bound[A]
    val dBlk = stageLambda1(cur){ notDone(cur) }
    val aBlk = stageLambda1(cur){ action(cur) }
    val nBlk = stageLambda1(cur){ nextState(cur) }
    stage(StateMachine(Set.empty, start, dBlk, aBlk, nBlk))
  }
}

