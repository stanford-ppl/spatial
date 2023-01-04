package spatial.executor.scala

import spatial.lang._
import spatial.node._

trait LoopedController {
  // A Looped Controller

  // Listed Outermost to Innermost
  var iterStates: Seq[(I32, IterState)]

  def isLastIter: Boolean = iterStates.forall(_._2.isLast)

  def stepForward(): Unit = {
    var advance: Boolean = true
    val newIterStatesR = iterStates.reverse.map {
      case (iter, iState@IterState(_, start, stop, step)) if iState.isLast && advance =>
        iter -> IterState(start, start, stop, step)
      case (iter, IterState(current, start, stop, step)) if advance =>
        advance = false
        iter -> IterState(current + step, start, stop, step)
      case (iter, iState) => iter -> iState
    }
    iterStates = newIterStatesR.reverse
  }
}
