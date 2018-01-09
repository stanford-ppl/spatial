package pcc
package spade

import scala.collection.mutable.ListBuffer

abstract class Node(implicit val state: State) {
  val id: Int = state.nextId()

  override def equals(that: Any): Boolean = that match {
    case n: Node => super.equals(n) && this.id == n.id
    case _ => super.equals(that)
  }
}

abstract class Module(implicit state: State) extends Node {
  implicit val parent: Option[Module] = Some(this)
  private val _children = ListBuffer[Module]()
}
object Module {
  def apply(x: Module)(implicit parent: Option[Module]): Unit = {

  }
}


abstract class Primitive(implicit state: State) extends Module {

}
