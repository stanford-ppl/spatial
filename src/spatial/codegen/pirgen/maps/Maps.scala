package spatial.codegen.pirgen

import scala.collection.Map
import scala.collection.Set

trait UniMap {
  type K
  type VV
  type V
  type M <: Map[K, VV]

  val map:M
  val name:String = this.getClass().getSimpleName().replace("$","")
  def apply(n:K):VV = { val m = map; m(n) }
  def get(n:K):Option[VV] =  { val m = map; m.get(n) }
  def foreach(f:((K, VV)) => Unit) = map.foreach(f)
  def contains(k:K) = map.contains(k)
  def quote(n:Any) = n.toString 
  def keys = map.keys
  def values = map.values
  def check(rec:(K,V)):Unit = {}
  def isMapped(v:V):Boolean
}

trait OneToOneMap extends UniMap {
  type VV = V
  def isMapped(v:V) = map.values.toList.contains(v)
  override def check(rec:(K,V)):Unit =  {
    if (map.contains(rec._1) && map(rec._1)!=rec._2)
      throw new Exception(s"${name} already contains key ${rec._1} -> ${map(rec._1)} but try to rebind to ${rec._2}")
  }
}

trait OneToManyMap extends UniMap {
  type VV <: Set[V]
  def isMapped(v:V) = map.values.toList.flatten.contains(v)
}
