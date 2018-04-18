package spatial.codegen.pirgen

import scala.collection.mutable.Map
import scala.collection.mutable.Set

trait MMap extends UniMap {
  override type M = Map[K,VV]
  def clear = { map.clear }
  def update(n:K, v:V):Unit
  def getOrElseUpdate(k:K)(v: => VV):VV
  def transform(f: (K, VV) ⇒ VV): M = map.transform(f)
  def filterNot(p: ((K, VV)) ⇒ Boolean) = map.filterNot(p)
  def retain(p: (K, VV) ⇒ Boolean): M = map.retain(p)
}

trait MBiMap extends BiMap with MMap {
  override type IM = Map[V, KK]
  override def clear = { super.clear; imap.clear }
}

trait MOneToOneMap extends OneToOneMap with MMap {
  override type M = Map[K, VV]
  val map:Map[K, VV] = Map.empty
  def update(n:K, v:V):Unit = { check((n,v)); map += (n -> v) }
  def getOrElseUpdate(k:K)(v: => VV):VV = {
    if (!map.contains(k)) update(k,v) 
    map(k)
  }
}

trait MBiOneToOneMap extends MOneToOneMap with BiOneToOneMap with MBiMap {
  override type IM = Map[V, KK]
  val imap:IM = Map.empty
  override def update(n:K, v:V):Unit = { check((n,v)); super.update(n, v); imap += (v -> n) }
  override def transform(f: (K, VV) ⇒ VV): M = {
    map.foreach { case (k, vv) =>
      imap -= vv
      val newVV = f(k, vv)
      imap += vv -> k
    }
    super.transform(f)
  } 
}

trait MOneToManyMap extends OneToManyMap with MMap {
  override type VV = Set[V]
  override type M = Map[K, VV]
  val map:Map[K, VV] = Map.empty
  def update(n:K, v:V):Unit = { check((n,v)); map.getOrElseUpdate(n, Set[V]()) += v }
  def getOrElseUpdate(k:K)(v: => VV):VV = {
    if (!map.contains(k)) v.foreach { v => update(k,v) }
    map(k)
  }
  def update(n:K, vv:VV):Unit = map += n -> vv
}

trait MBiOneToManyMap extends MOneToManyMap with BiOneToManyMap with MBiMap {
  override type IM = Map[V, KK]
  val imap:IM = Map.empty
  override def update(n:K, v:V):Unit = { check((n,v)); super.update(n,v); imap += (v -> n) } 
  override def update(n:K, vv:VV):Unit = {
    map(n).foreach { v => imap.remove(v) }
    super.update(n, vv)
    vv.foreach { v => imap += v -> n }
  }
  override def transform(f: (K, VV) ⇒ VV): M = {
    map.foreach { case (k, vv) =>
      vv.foreach { v => imap -= v }
      val newVV = f(k, vv)
      newVV.foreach { v => imap += v -> k }
    }
    super.transform(f)
  } 
}

trait MBiManyToOne extends MOneToOneMap with BiManyToOneMap with MMap {
  override type KK = Set[K]
  override type IM = Map[V, KK]
  val imap:IM = Map.empty
  override def update(n:K, v:V):Unit = { check((n,v)); super.update(n,v); imap.getOrElseUpdate(v, Set[K]()) += n } 
}

trait MBiManyToMany extends MOneToManyMap with BiManyToManyMap with MBiMap {
  override type KK = Set[K]
  override type IM = Map[V, KK]
  val imap:IM = Map.empty
  override def update(n:K, v:V):Unit = { super.update(n,v); imap.getOrElseUpdate(v, Set[K]()) += n } 
}
