package pcc.core

import scala.collection.mutable

class GraphMetadata {
  type HMap[K,V] = mutable.HashMap[K,V]
  def HMap[K,V](): HMap[K,V] = new mutable.HashMap[K,V]()

  private def keyOf[M<:Metadata[M]:Manifest]: Class[M] = manifest[M].runtimeClass.asInstanceOf[Class[M]]

  type GraphData  = HMap[Int, SymbolData]
  private def GraphData() = HMap[Int,SymbolData]()

  type SymbolData = HMap[Class[_],Metadata[_]]
  private def SymbolData() = HMap[Class[_],Metadata[_]]()

  /**
    * Maintained as an Array of f Maps. An Array of Sets nearly works, but requires either making the equals method
    * on metadata instances otherwise useless, or spending linear time searching for existing entries with the same key
    */
  private val edgeMetadata = HMap[Int, SymbolData]()

  final def getAllMetadata(edge: Sym[_]): Option[SymbolData] = edgeMetadata.get(edge.id)

  final def getOrElseCreate(edge: Sym[_]): SymbolData = getAllMetadata(edge) match {
    case Some(map) => map
    case None =>
      val map = SymbolData()
      edgeMetadata(edge.id) = map
      map
  }

  final def getMetadata[T](edge: Sym[_], key: Class[T]): Option[T] = {
    getAllMetadata(edge).flatMap(_.get(key)).map(_.asInstanceOf[T])
  }

  final def addMetadata(edge: Sym[_], m: Metadata[_]): Unit = {
    val map = getOrElseCreate(edge)
    map += m.key -> m
  }
  final def removeMetadata(edge: Sym[_], key: Class[_]): Unit = if (edgeMetadata.contains(edge.id)) {
    val map = getOrElseCreate(edge)
    if (map.contains(key)) map.remove(key)
  }

  final def clearMetadata(m: Class[_]): Unit = edgeMetadata.keys.foreach{k =>
    val map = edgeMetadata(k)
    if (map.contains(m)) map.remove(m)
  }

  def reset(): Unit = { edgeMetadata.clear() }

  def copyTo(that: GraphMetadata): GraphMetadata = {
    that.reset()
    edgeMetadata.foreach{case (k,map) =>
      val map2 = SymbolData()
      map2 ++= map
      that.edgeMetadata += k -> map2
    }
    that
  }

  def addAll(edge: Sym[_], data: Iterable[Metadata[_]]): Unit = data.foreach{m => addMetadata(edge, m) }
  def addOrRemoveAll(edge: Sym[_], data: Iterable[(Class[_],Option[Metadata[_]])]): Unit = data.foreach{
    case (key,Some(m)) => addMetadata(edge, m)
    case (key,None)    => removeMetadata(edge,key)
  }

  def add[M<:Metadata[M]:Manifest](edge: Sym[_], m: M): Unit = addMetadata(edge, m)
  def add[M<:Metadata[M]:Manifest](edge: Sym[_], m: Option[M]): Unit = m match {
    case Some(data) => addMetadata(edge, data)
    case None => removeMetadata(edge, keyOf[M])
  }
  def all(edge: Sym[_]): Iterable[(Class[_],Metadata[_])] = getAllMetadata(edge).getOrElse(Nil)

  def apply[M<:Metadata[M]:Manifest](edge: Sym[_]): Option[M] = getMetadata(edge,keyOf[M])

  def clear[M<:Metadata[M]:Manifest]: Unit = clearMetadata(keyOf[M])
}





