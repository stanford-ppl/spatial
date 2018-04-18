package spatial.codegen.pirgen

import scala.collection.immutable.Map
import scala.collection.immutable.Set

trait IMapObj {
  type K
  type VV
  type V
  type M = Map[K,VV]
}
trait IBiMapObj extends IMapObj {
  type KK
  type IM = Map[V,KK]
}

trait IOneToOneMap extends OneToOneMap {
  override type M = Map[K, VV]
  def + (rec:(K,V)):IOneToOneMap = new IOneToOneMap {
    super.check(rec)
    type K = IOneToOneMap.this.K
    type V = IOneToOneMap.this.V
    val map = IOneToOneMap.this.map + rec 
  }
}
trait IOneToOneObj extends IMapObj {
  type VV = V
}

trait IBiOneToOneMap extends IOneToOneMap with BiOneToOneMap {
  override type IM = Map[V, KK]
  override def + (rec:(K,V)):IBiOneToOneMap = new IBiOneToOneMap {
    super.check(rec)
    type K = IBiOneToOneMap.this.K
    type V = IBiOneToOneMap.this.V
    val map = IBiOneToOneMap.this.map + rec 
    val imap = IBiOneToOneMap.this.imap + rec.swap
  }
}
trait IBiOneToOneObj extends IOneToOneObj with IBiMapObj {
  type KK = K
}

trait IOneToManyMap extends OneToManyMap {
  override type VV = Set[V]
  override type M = Map[K, VV]
  def + (rec:(K,V)):IOneToManyMap = new IOneToManyMap {
    type K = IOneToManyMap.this.K
    type V = IOneToManyMap.this.V
    val mp = IOneToManyMap.this.map
    val set = mp.getOrElse(rec._1, Set.empty) + rec._2
    val map = mp + (rec._1 -> set)
  }
}

trait IOneToManyObj extends IMapObj {
  type VV = Set[V]
}

trait IBiOneToManyMap extends IOneToManyMap with BiOneToManyMap {
  override type IM = Map[V, KK]
  override def + (rec:(K,V)):IBiOneToManyMap = new IBiOneToManyMap {
    super.check(rec)
    type K = IBiOneToManyMap.this.K
    type V = IBiOneToManyMap.this.V
    val mp = IBiOneToManyMap.this.map
    val set = mp.getOrElse(rec._1, Set.empty) + rec._2
    val map = mp + (rec._1 -> set)
    val imap = IBiOneToManyMap.this.imap + rec.swap
  }
}

trait IBiOneToManyObj extends IOneToManyObj with IBiMapObj {
  type KK = K
}

trait IBiManyToOneMap extends IOneToOneMap with BiManyToOneMap {
  override type KK = Set[K]
  override type IM = Map[V, KK]
  override def + (rec:(K,V)):IBiManyToOneMap = new IBiManyToOneMap {
    super.check(rec)
    type K = IBiManyToOneMap.this.K
    type V = IBiManyToOneMap.this.V
    val map = IBiManyToOneMap.this.map + rec 
    val pmp = IBiManyToOneMap.this.imap
    val pset = pmp.getOrElse(rec._2, Set.empty) + rec._1
    val imap = pmp + (rec._2 -> pset)
  }
}

trait IBiManyToOneObj extends IOneToOneObj with IBiMapObj {
  type KK = Set[K]
}

trait IBiManyToManyMap extends IOneToManyMap with BiManyToManyMap {
  override type KK = Set[K]
  override type IM = Map[V, KK]
  override def + (rec:(K,V)):IBiManyToManyMap = new IBiManyToManyMap {
    type K = IBiManyToManyMap.this.K
    type V = IBiManyToManyMap.this.V
    val mp = IBiManyToManyMap.this.map
    val set = mp.getOrElse(rec._1, Set.empty) + rec._2
    val map = mp + (rec._1 -> set)
    val pmp = IBiManyToManyMap.this.imap
    val pset = pmp.getOrElse(rec._2, Set.empty) + rec._1
    val imap = pmp + (rec._2 -> pset)
  }
}
trait IBiManyToManyObj extends IOneToManyObj with IBiMapObj {
  type KK = Set[K]
}
