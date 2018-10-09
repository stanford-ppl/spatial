package spatial.codegen.pirgen

import argon._
import argon.codegen.Codegen
import spatial.metadata.memory._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable

trait PIRGenHelper extends PIRFormatGen {

  def assertOne[T](vec:Seq[T]):T = {
    assert(vec.size==1, s"More than one element in vector $vec for pir backend")
    vec.head
  }

  def stateStruct[C[_]](lhs:Sym[_], A:Type[_], tp:Option[String]=None)(rhs:Option[String] => Any):Unit = {
    A match {
      case a:Struct[_] =>
        a.fields.foreach { case (name, _) =>
          state(Lhs(lhs, Some(name)), tp=tp)(rhs(Some(name)))
        }
      case a => state(lhs)(rhs(None))
    }
  }

  def stateRead(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Sym[_]]], ens:Seq[Set[Bit]]) = {
    stateStruct(lhs, mem.asMem.A){ name => 
      src"ReadMem(${Lhs(mem,name)}, bank=${bank.map(assertOne)}, offset=${ofs.map(assertOne)}, ens=${assertOne(ens)})"
    }
  }

  def stateWrite(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Sym[_]]], data:Seq[Sym[_]], ens:Seq[Set[Bit]]) = {
    stateStruct(lhs, mem.asMem.A){ name => 
      src"WriteMem(${Lhs(mem,name)}, bank=${bank.map(assertOne)}, offset=${ofs.map(assertOne)}, data=${Lhs(assertOne(data), name)}, ens=${assertOne(ens)})"
    }
  }
}
