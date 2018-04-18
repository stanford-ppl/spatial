package spatial.codegen.pirgen

import scala.collection.Map
import scala.collection.Set

trait BiMap extends UniMap {
  type KK
  type IM <: Map[V, KK]
  protected def imap:IM
  override def isMapped(v:V) = imap.contains(v)

  def icontains(v:V) = imap.contains(v)
  def iget(v:V) = imap.get(v)
}

trait BiOneToOneMap extends OneToOneMap with BiMap {
  type KK = K
  override def check(rec:(K,V)):Unit =  {
    super.check(rec)
    if (imap.contains(rec._2) && imap(rec._2)!=rec._1)
      throw new Exception(s"${name} already contains key ${rec._2} -> ${imap(rec._2)} but try to rebind to ${rec._1}")
  }
}

trait BiOneToManyMap extends OneToManyMap with BiMap {
  type KK = K
  override def check(rec:(K,V)):Unit =  {
    if (imap.contains(rec._2) && imap(rec._2)!=rec._1)
      throw new Exception(s"${name} already contains key ${rec._2} -> ${imap(rec._2)} but try to rebind to ${rec._1}")
  }
}

trait BiManyToOneMap extends OneToOneMap with BiMap {
  type KK <: Set[K]
  override def check(rec:(K,V)):Unit = {
    if (map.contains(rec._1) && map(rec._1) != rec._2)
      throw new Exception(s"${name} already contains key ${rec._1} -> ${map(rec._1)} but try to rebind to ${rec._2}")
  }
}

trait BiManyToManyMap extends OneToManyMap with BiMap {
  type KK <: Set[K]
  val imap:IM
}
