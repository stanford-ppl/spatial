package argon

import argon.transform.Transformer

import scala.collection.mutable

class GlobalMetadata {
  type Tx = Transformer

  private def keyOf[M<:Metadata[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  private val data = mutable.HashMap[Class[_],Metadata[_]]()

  private def add(k: Class[_], m: Metadata[_]): Unit = { data += k -> m }
  def add[M<:Metadata[M]:Manifest](m : M): Unit = { data += m.key -> m }
  def apply[M<:Metadata[M]:Manifest]: Option[M] = data.get(keyOf[M]).map(_.asInstanceOf[M])
  def clear[M<:Metadata[M]:Manifest]: Unit = data.remove(keyOf[M])

  def mirrorAfterTransform(f:Tx): Unit = data.foreach{case (k,v) => Option(v.mirror(f)) match {
    case Some(v2) => data(k) = v2.asInstanceOf[Metadata[_]]
    case None => data.remove(k)
  }}
  def clearBeforeTransform(): Unit = {
    val remove = data.collect{case (k,v) if v.skipOnTransform => k }
    remove.foreach{k => data.remove(k) }
  }

  def copyTo(that: GlobalMetadata): Unit = data.foreach{case (k,v) => that.add(k,v) }
  def reset(): Unit = data.clear()

  def foreach(func: (Class[_],Metadata[_]) => Unit): Unit = data.foreach{case (k,v) => func(k,v)}
}
