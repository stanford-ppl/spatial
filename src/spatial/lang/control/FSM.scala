package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._

object FSM {
  @api def apply[A:Bits](init: Lift[A])(notDone: A => Bit)(action: A => Void)(next: A => A): Void = {
    fsm(init.unbox, notDone, action, next)
  }

  @api def apply[A](init: Bits[A])(notDone: A => Bit)(action: A => Void)(next: A => A): Void = {
    implicit val A: Bits[A] = init.selfType
    fsm(init.unbox, notDone, action, next)
  }

  @rig def fsm[A:Bits](start: A, notDone: A => Bit, action: A => Void, nextState: A => A): Void = {
    val cur = boundVar[A]
    val dBlk = stageLambda1(cur){ notDone(cur) }
    val aBlk = stageLambda1(cur){ action(cur) }
    val nBlk = stageLambda1(cur){ nextState(cur) }
    stage(StateMachine(Set.empty, start, dBlk, aBlk, nBlk))
  }
}

