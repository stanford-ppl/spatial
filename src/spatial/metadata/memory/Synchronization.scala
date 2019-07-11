package spatial.metadata.memory

import argon._
import scala.collection.mutable.ListBuffer

/** Set barrier of an write access
  *
  * Getter:  sym.barrier = Option[Int]
  * Setter:  sym.barrier = Int
  * Default: None
  */
case class Barrier(id: Int) extends Data[Barrier](SetBy.User)

/** Set barrier of an write access
  *
  * Getter:  sym.barrier = Option[Int]
  * Setter:  sym.barrier = Int
  * Default: None
  */
case class Wait(ids: ListBuffer[Int] = ListBuffer.empty) extends Data[Wait](SetBy.User)
