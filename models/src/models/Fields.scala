package models

import scala.language.higherKinds

trait Fields[T,M[_]<:Fields[_,M]] {
  def fields: Array[String]
  def default: T
  def convert[B](d: B): M[B]
}

case class AreaFields[T](fields: Array[String], default: T) extends Fields[T,AreaFields] {
  def convert[B](d: B): AreaFields[B] = AreaFields[B](fields, d)
}

case class LatencyFields[T](fields: Array[String], default: T) extends Fields[T,LatencyFields] {
  def convert[B](d: B): LatencyFields[B] = LatencyFields(fields, d)
}
