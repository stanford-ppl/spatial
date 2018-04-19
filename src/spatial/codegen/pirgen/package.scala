package spatial.codegen

import argon._
import spatial.lang._
import spatial.node._
import spatial.data._
import emul.{FixedPoint,FloatPoint}

package object pirgen {

  val metadatas = scala.collection.mutable.ListBuffer[MetadataMaps]()

  def isConstant(x: Sym[_]):Boolean = x match {
    case Const(c) => true
    case Param(c) => true
    case Final(c) => true
    case _ => false
  }

  def getConstant(x: Sym[_]): Option[Any] = x match {
    case Const(c) => Some(c)
    case Param(c) => Some(c)
    case Final(c)  => Some(c)
    case _ => None
  }
}
