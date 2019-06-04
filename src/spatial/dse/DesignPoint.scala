	package spatial.dse

import spatial.metadata._
import spatial.metadata.params._
import spatial.metadata.bounds._
import forge.tags.stateful

abstract class DesignPoint {
  @stateful def set(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Unit
  @stateful def show(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Seq[Any]
}


case class PointIndex(pt: BigInt) extends DesignPoint {
  @stateful def set(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Unit = {
    indexedSpace.foreach{case (domain,d) => domain.set( ((pt / prods(d)) % dims(d)).toInt ) }
  }
  @stateful def show(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Seq[Any] = {
  	indexedSpace.flatMap{case (domain,d) => Seq(domain.name, domain.options( ((pt / prods(d)) % dims(d)).toInt ))}
  } 
}

case class Point(params: Seq[AnyVal]) extends DesignPoint {
  @stateful def set(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Unit = {
    indexedSpace.foreach{case (domain,d) => domain.setValueUnsafe(params(d)) }
  }
  @stateful def show(indexedSpace: Seq[(Domain[_], Int)], prods: Seq[BigInt], dims: Seq[BigInt]): Seq[Any] = {
    params.zipWithIndex.map{case (p,i) => Seq(indexedSpace(i)._1.name, p)}
  } 
}