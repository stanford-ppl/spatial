package spatial.traversal.routing

import forge.tags._
import argon._

import scala.collection.mutable
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}

trait UniformCostSearch {
  class PlacerError(val msg: String) extends Error with NoStackTrace
  class SearchFailure(override val msg: String) extends PlacerError(msg)

  // (S, A, C): (State, Action, Cost)
  type BackPointer[S,A,C] = mutable.Map[S, (S,A,C)]
  type Explored[S] = mutable.ListBuffer[S]
  type AdvanceFunc[S,A,C] = (S,BackPointer[S,A,C],C) => Seq[(S, A, C)]

  /**
   * Find the minimum path from start to end
   * Call finPass when a route is found. If finPass is succeeded, return mapping from finPass.
   * If finPass fails, continue find routes. Throw exception when no route is found
   * @return mapping
   */
  @stateful def uniformCostSearch[S, A, C:Ordering, M](
    start:    S,
    isEnd:    S => Boolean,
    zeroCost: C,
    sumCost:  (C,C) => C,
    advance:  AdvanceFunc[S,A,C],
    quote:    S => String,
    finPass:  (List[(S,A)], C) => M
  ): Either[Error, M] = {

    def terminate(minNode: S, explored: Explored[S], backPointers: BackPointer[S,A,C]): Option[M] = {
      if (isEnd(minNode)) {
        assert(explored.toSet.size == explored.size)
        val (route, cost) = extractHistory(start, minNode, backPointers, zeroCost, sumCost)
        Try(finPass(route, cost)) match {
          case Success(m) => Some(m)
          case Failure(e: SearchFailure) => // Continue
            //explored.clear
            //backPointers.clear
            //frontier.clear
            //frontier += State(start, zeroCost)
            dbg(s"$e")
            None
          case Failure(e) => throw e
        }
      } else {
        explored += minNode
        None
      }
    }

    def cleanUp(explored:Explored[S], backPointers:BackPointer[S,A,C]): Either[Error, M] = {
      Left(new PlacerError(s"No route from ${quote(start)}"))
    }

    uniformCostTraverse(
      start=start,
      zeroCost=zeroCost,
      sumCost=sumCost,
      advance=advance,
      quote=quote,
      terminate=terminate,
      cleanUp=cleanUp
    )
  }

  /**
   * Find list of nodes reachable from start
   */
  @stateful def uniformCostSpan[S,A,C:Ordering](
    start:    S,
    zeroCost: C,
    sumCost:  (C,C) => C,
    advance:  AdvanceFunc[S,A,C],
    quote:    S => String
  ):Seq[(S,C)] = {

    def terminate(minNode: S, explored: mutable.ListBuffer[S], backPointers:BackPointer[S,A,C]): Option[Seq[(S,C)]] = {
      explored += minNode
      None
    }

    def cleanUp(explored: Explored[S], backPointers: BackPointer[S,A,C]): Either[Error, Seq[(S,C)]] = {
      assert(explored.toSet.size == explored.size)
      Right(explored.map { n => (n, extractHistory(start, n, backPointers, zeroCost, sumCost)._2) }.toList)
    }

    uniformCostTraverse(
      start=start,
      zeroCost=zeroCost,
      sumCost=sumCost,
      advance=advance,
      quote=quote,
      terminate=terminate,
      cleanUp=cleanUp
    ).right.get
  }

  @stateful def uniformCostTraverse[S, A, C:Ordering,M](
    start:     S,
    zeroCost:  C,
    sumCost:   (C,C) => C,
    advance:   AdvanceFunc[S,A,C],
    quote:     S => String,
    terminate: (S, Explored[S], BackPointer[S,A,C]) => Option[M],
    cleanUp:   (Explored[S], BackPointer[S,A,C]) => Either[Error, M]
  ): Either[Error, M] = {

    case class State(n:S, var cost:C) extends Ordered[State] {
      override def toString = s"State(${quote(n)}, $cost)"
      def compare(that:State):Int = -implicitly[Ordering[C]].compare(cost, that.cost)
    }

    val explored:Explored[S] = mutable.ListBuffer[S]()

    val backPointers:BackPointer[S,A,C] = mutable.Map[S, (S,A,C)]()

    var frontier = mutable.PriorityQueue[State]()

    frontier += State(start, zeroCost)

    while (frontier.nonEmpty) {
      dbg(s"frontier: ")
      dbg(s"- $frontier")
      dbg("")

      val State(minNode, pastCost) = frontier.dequeue()

      dbg(s"${quote(minNode)}")
      dbg(s"  pastCost: $pastCost")

      terminate(minNode, explored, backPointers).foreach { res =>
        dbg(s"")
        return Right(res) // why is this cast necessary?
      }

      var neighbors = advance(minNode, backPointers, pastCost)

      //logger.foreach { l =>
      //l.dprintln(s"neighbors:")
      //l.dprintln(s" - ${neighbors.map { case (n, a, c) => s"(${quote(n)}, $c)" }.mkString(",")}")
      //}

      neighbors = neighbors.filterNot { case (n, a, c) => explored.contains(n) }

      //logger.foreach { l =>
      //l.dprintln(s"neighbors not explored:")
      //l.dprintln(s" - ${neighbors.map { case (n, a, c) => s"(${quote(n)}, $c)" }.mkString(",")}")
      //}

      neighbors = neighbors.groupBy { case (n, a, c) => n }.map { case (n, groups) =>
        groups.minBy { case (n, a, c) => c }
      }.toSeq

      dbg(s"neighbors minBy:")
      dbg(s" - ${neighbors.map { case (n,a,c) => s"(${quote(n)}, $c)" }.mkString(",")}")

      neighbors.foreach { case (neighbor, action, cost) =>
        val newCost = sumCost(pastCost, cost)
        frontier.find{case State(n,c) => n == neighbor }.fold[Unit]{
          frontier += State(neighbor, newCost)
          backPointers += neighbor -> ((minNode, action, cost))
        } { node =>
          if (implicitly[Ordering[C]].lt(newCost, node.cost)) {
            node.cost = newCost
            backPointers += neighbor -> ((minNode, action, cost))
            frontier = frontier.clone() // Create a new copy to force reordering
          }
        }
      }
      dbg("")
    }

    cleanUp(explored, backPointers)
  }

  def extractHistory[S,A,C](
    start:        S,
    end:          S,
    backPointers: BackPointer[S,A,C],
    zeroCost:     C,
    sumCost:      (C,C) => C
  ):(List[(S,A)], C) = {
    var totalCost = zeroCost
    val history = mutable.ListBuffer[(S, A)]()
    var current = end
    while (current != start) {
      val (prevNode, action, cost) = backPointers(current)
      totalCost = if (totalCost==null) cost else sumCost(totalCost, cost)
      history += ((current, action))
      current = prevNode
    }
    (history.toList.reverse, totalCost)
  }

}

