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

  def stateMem(lhs:Sym[_], rhs:String, inits:Option[Seq[Sym[_]]]=None, tp:Option[String]=None) = {
    val padding = lhs.getPadding.getOrElse {
      lhs.constDims.map { _ => 0 }
    }
    stateStruct(lhs, lhs.asMem.A, tp=tp)(name => 
      src"$rhs" + 
      inits.ms(inits => src".inits($inits)") + 
      src".depth(${lhs.instance.depth})" +
      src".dims(${lhs.constDims.zip(padding).map { case (d,p) => d + p }})" +
      src".banks(${lhs.instance.banking.map { b => b.nBanks}})" 
    )
  }

  def stateStruct[C[_]](lhs:Sym[_], A:Type[_], tp:Option[String]=None)(rhsFunc:Option[String] => Any):Unit = {
    A match {
      case a:Struct[_] =>
        a.fields.foreach { case (name, _) =>
          rhsFunc(Some(name)) match {
            case rhs:Lhs if (typeMap.contains(rhs)) => alias(Lhs(lhs, Some(name)))(rhs)
            case rhs:Sym[_] if (typeMap.contains(rhs)) => alias(Lhs(lhs, Some(name)))(rhs)
            case rhs => state(Lhs(lhs, Some(name)), tp=tp)(rhs)
          }
        }
      case a => state(lhs, tp=tp)(rhsFunc(None))
    }
  }

  def stateRead(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], ens:Seq[Set[Bit]]) = {
    stateStruct(lhs, mem.asMem.A){ name => 
      (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedRead().mem(${Lhs(mem,name)}).en(${assertOne(ens)}).bank(${assertOne(bank)}).offset(${assertOne(ofs)})"
        case _ => 
          src"MemRead().mem(${Lhs(mem,name)}).en(${assertOne(ens)})"
      }
    }
  }

  def stateWrite(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], data:Seq[Sym[_]], ens:Seq[Set[Bit]]) = {
    stateStruct(lhs, mem.asMem.A){ name => 
      (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedWrite().mem(${Lhs(mem,name)}).en(${assertOne(ens)}).bank(${assertOne(bank)}).offset(${assertOne(ofs)}).data(${Lhs(assertOne(data), name)})"
        case _ => 
          src"MemWrite().mem(${Lhs(mem,name)}).en(${assertOne(ens)}).data(${Lhs(assertOne(data), name)})"
      }
    }
  }

  def genOp(lhs:Sym[_], op:Option[String]=None, inputs:Option[Seq[Any]]=None) = {
    val rhs = lhs.op.get
    val opStr = op.getOrElse(rhs.getClass.getSimpleName)
    val ins = inputs.getOrElse(rhs.productIterator.toList)
    state(lhs)(src"""OpDef(op="$opStr").input($ins)""")
  }

  implicit class OptionHelper[T](opt:Option[T]) {
    def ms(func: T => String) = opt.fold("")(func)
  }

}
