package spatial

import argon._
import spatial.metadata.access._
import poly.{ISL, ConstraintMatrix}

trait SpatialTestbench extends argon.DSLTestbench {
  class SpatialISL extends ISL {
    override def domain[K](key: K): ConstraintMatrix[K] = key match {
      case s: Sym[_] => s.domain.asInstanceOf[ConstraintMatrix[K]]
      case _ => throw new Exception(s"Cannot get domain of $key")
    }
  }

  override def initConfig(): argon.Config = new SpatialConfig
}
