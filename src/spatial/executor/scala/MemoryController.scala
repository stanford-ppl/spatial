package spatial.executor.scala

import scala.collection.{mutable => cm}


sealed trait RequestStatus
case class RequestPending(cycles: Int) extends RequestStatus
case class RequestTransferring(bytesRemaining: Int) extends RequestStatus
case object RequestFinished extends RequestStatus

class Request(val size: Int, var status: RequestStatus) {
  def tick(): Unit = {
    status = status match {
      case RequestPending(0) => RequestTransferring(size)
      case RequestPending(n) => RequestPending(n-1)
      case x => x
    }
  }
}

object Request {
  def unapply(request: Request): Option[(Int, RequestStatus)] = Some((request.size, request.status))
}


class MemoryController(bytesPerTick: Int, responseLatency: Int, maxActiveRequests: Int) {
  // maintain an internal queue of requests
  private val activeRequests = cm.Queue[Request]()
  private val backlog = cm.Queue[Request]()

  def makeRequest(size: Int): Request = {
    val req = new Request(size, RequestPending(responseLatency))
    backlog.enqueue(req)
    req
  }

  def tick(): Unit = {
    while (activeRequests.size < maxActiveRequests && backlog.nonEmpty) {
      activeRequests.enqueue(backlog.dequeue())
    }
    activeRequests.foreach(_.tick())
    var remainingBytes = bytesPerTick
    while (remainingBytes > 0) {
      activeRequests.headOption match {
        case None => return
        case Some(Request(_, RequestPending(_))) => return
        case Some(rq@Request(i, RequestTransferring(remaining))) =>
          val transferred = scala.math.min(remainingBytes, remaining)
          remainingBytes -= transferred
          if (transferred == remaining) {
            rq.status = RequestFinished
            activeRequests.dequeue()
          } else {
            rq.status = RequestTransferring(remaining - transferred)
          }
      }
    }
  }

  def print(println: String => Any): Unit = {
    println(s"Currently Active Requests: ${activeRequests.map(_.toString).mkString(", ")}")
    println(s"Backlog: ${backlog.map(_.toString).mkString(", ")}")
  }
}
