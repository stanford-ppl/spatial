package spatial.codegen.pirgen

import argon._
import argon.codegen.Codegen
import spatial.metadata.memory._
import spatial.metadata.bounds._
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
    val constInits = inits.map { _.map { _.rhs.getValue.get } }.flatMap { inits =>
      if (inits.size > 16) None else Some(inits) //TODO: hack. prevent generating inits that are too large
    }
    stateStruct(lhs, lhs.asMem.A, tp=tp)(name => 
      src"$rhs" + 
      constInits.ms(constInits => src".inits($constInits)") + 
      src".depth(${lhs.instance.depth})" +
      src".dims(${lhs.constDims.zip(padding).map { case (d,p) => d + p }})" +
      src".banks(${lhs.instance.banking.map { b => b.nBanks}})" 
    )
  }

  def stateStruct[C[_]](lhs:Sym[_], A:Type[_], tp:Option[String]=None)(rhsFunc:Option[String] => Any):Unit = {
    A match {
      case a:Struct[_] =>
        a.fields.foreach { case (name, _) =>
          stateOrAlias(Lhs(lhs, Some(name)), tp)(rhsFunc(Some(name)))
        }
      case a => 
        stateOrAlias(lhs, tp)(rhsFunc(None))
    }
  }

  def stateRead(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], ens:Seq[Set[Bit]]) = {
    val bufferPort = lhs.port.bufferPort
    val muxPort = lhs.port.muxPort
    stateStruct(lhs, mem.asMem.A){ name => 
      (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedRead().setMem(${Lhs(mem,name)}).en(${assertOne(ens)}).bank(${assertOne(bank)}).offset(${assertOne(ofs)}).port($bufferPort).muxPort($muxPort)"
        case _ => 
          src"MemRead().setMem(${Lhs(mem,name)}).en(${assertOne(ens)}).port($bufferPort).muxPort($muxPort)"
      }
    }
  }

  def stateWrite(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], data:Seq[Sym[_]], ens:Seq[Set[Bit]]) = {
    val bufferPort = lhs.port.bufferPort
    val muxPort = lhs.port.muxPort
    stateStruct(lhs, mem.asMem.A){ name => 
      (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedWrite().setMem(${Lhs(mem,name)}).en(${assertOne(ens)}).bank(${assertOne(bank)}).offset(${assertOne(ofs)}).data(${Lhs(assertOne(data), name)}).port($bufferPort).muxPort($muxPort)"
        case _ => 
          src"MemWrite().setMem(${Lhs(mem,name)}).en(${assertOne(ens)}).data(${Lhs(assertOne(data), name)}).port($bufferPort).muxPort($muxPort)"
      }
    }
  }

  def genOp(lhs:Lhs, op:Option[String]=None, inputs:Option[Seq[Any]]=None) = {
    val rhs = lhs.sym.op.get
    val opStr = op.getOrElse(rhs.getClass.getSimpleName)
    val ins = inputs.getOrElse(rhs.productIterator.toList)
    state(lhs)(src"""OpDef(op=$opStr).input($ins)""")
  }

  implicit class OptionHelper[T](opt:Option[T]) {
    def ms(func: T => String) = opt.fold("")(func)
  }

}
