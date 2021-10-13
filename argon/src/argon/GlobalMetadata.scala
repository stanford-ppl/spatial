package argon

import argon.transform.Transformer

import scala.collection.mutable

class GlobalMetadata {
  type Tx = Transformer

  private def keyOf[M<:Data[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  private val data = mutable.HashMap[Class[_],Data[_]]()

  private def add(k: Class[_], m: Data[_]): Unit = { data += k -> m }
  def add[M<:Data[M]:Manifest](m : M): Unit = { data += m.key -> m }
  def apply[M<:Data[M]:Manifest]: Option[M] = data.get(keyOf[M]).map(_.asInstanceOf[M])
  def clear[M<:Data[M]:Manifest]: Unit = data.remove(keyOf[M])

  def invalidateBeforeTransform(): Unit = {
    val remove = data.filter{case (k,v) => v.transfer match {
      case Transfer.Remove => true
      case Transfer.Mirror => false
      case Transfer.Ignore => false
    }}
    remove.foreach{k => data.remove(k._1) }
  }

  private def sortedData = data.toList.sortBy {_._1.toString}

  def copyTo(that: GlobalMetadata): Unit = data.foreach{case (k,v) => that.add(k,v) }
  def reset(): Unit = data.clear()

  def foreach(func: (Class[_],Data[_]) => Unit): Unit = sortedData.foreach{case (k,v) => func(k,v)}
}
