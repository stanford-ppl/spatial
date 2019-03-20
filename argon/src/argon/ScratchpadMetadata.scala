package argon

import argon.transform.Transformer

import scala.collection.mutable

class ScratchpadMetadata {

  private def keyOf[M<:Data[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  private val data = mutable.HashMap[(Sym[_],Class[_]),Data[_]]()

  def add(edge: Sym[_], key: Class[_], m: Data[_]): Unit = data += (((edge,key) -> m))

  def add[M<:Data[M]:Manifest](edge: Sym[_], m: M): Unit = data += (((edge, m.key), m))
  def add[M<:Data[M]:Manifest](edge: Sym[_], m: Option[M]): Unit = m match {
    case Some(dat) => data += (((edge, dat.key), dat))
    case None => data.remove(edge, keyOf[M])
  }

  override def toString: String = {
    data.map{case ((s,c), d) => s"($s, $c) -> $d"}.mkString(", ")
  }

  def remove(edge: Sym[_], key: Class[_]): Unit = data.remove((edge, key))

  def clear[M<:Data[M]:Manifest](edge: Sym[_]): Unit = data.remove(edge, keyOf[M])

  def apply[M<:Data[M]:Manifest](edge: Sym[_]): Option[M] = data.get((edge, keyOf[M])).map(_.asInstanceOf[M])

}
