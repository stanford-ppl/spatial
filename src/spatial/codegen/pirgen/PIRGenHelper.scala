package spatial.codegen.pirgen

import argon._
import argon.codegen.Codegen
import spatial.metadata.memory._
import spatial.metadata.bounds._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable

trait PIRGenHelper extends PIRFormatGen {

  def assertOne[T](vec:Iterable[T]):T = {
    if (vec.size != 1) {
      //error(s"More than one element in vector $vec for pir backend")
      //IR.logError()
      throw new Exception(s"More than one element in vector $vec for pir backend")
    }
    vec.head
  }

  def stateMem(lhs:Sym[_], rhs:String, inits:Option[Any]=None, tp:Option[String]=None) = {
    val padding = lhs.getPadding.getOrElse {
      lhs.constDims.map { _ => 0 }
    }
    stateStruct(lhs, lhs.asMem.A, tp=tp)(field => 
      src"$rhs" + 
      inits.ms(inits => src".inits($inits)") + 
      src".depth(${lhs.instance.depth})" +
      src".dims(${lhs.constDims.zip(padding).map { case (d,p) => d + p }})" +
      src".darkVolume(${lhs.getDarkVolume.getOrElse(0)})" +
      src".banks(${lhs.instance.banking.map { b => b.nBanks}})" +
      src".tp(${field.map {_._2}.getOrElse(lhs.asMem.A)})" + 
      src".isInnerAccum(${lhs.isInnerAccum})"
    )
  }

  def stateStruct(lhs:Sym[_], A:Type[_], tp:Option[String]=None)(rhsFunc:Option[(String, Type[_])] => Any):Unit = {
    mapStruct(A) { 
      case s@Some((fieldName, t)) => stateOrAlias(Lhs(lhs, Some(fieldName)))(rhsFunc(s))
      case s@None => stateOrAlias(lhs, tp)(rhsFunc(s))
    }
  }

  def mapStruct[T](A:Type[_])(func:Option[(String, Type[_])] => T):Seq[T] = {
    A match {
      case a:Struct[_] =>
        a.fields.map { case (fieldName, t) => func(Some((fieldName, t))) }
      case a => 
        Seq(func(None))
    }
  }

  def stateRead(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], ens:Seq[Set[Bit]]) = {
    val bufferPort = lhs.port.bufferPort
    val broadcast = lhs.port.broadcast
    val castgroup = lhs.port.castgroup
    val muxPort = lhs.port.muxPort
    stateStruct(lhs, mem.asMem.A){ field => 
      val name = field.map { _._1 }
      var body = (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedRead()" +
          src".bank(${assertOne(bank)})" + 
          src".offset(${assertOne(ofs)})"
        case _ => 
          src"MemRead()"
      }
      body += 
        src".setMem(${Lhs(mem,name)})" + 
        src".en(${assertOne(ens)})" + 
        src".port($bufferPort)" + 
        src".muxPort($muxPort)" +
        src".broadcast($broadcast)" + 
        src".castgroup($castgroup)" + 
        src".tp(${field.map{_._2}.getOrElse(lhs.tp)})" +
        (if (lhs.sym.isInnerReduceOp) ".isInnerReduceOp(true)" else "")
      body
    }
  }

  def stateWrite(lhs:Sym[_], mem:Sym[_], bank:Option[Seq[Seq[Sym[_]]]], ofs:Option[Seq[Any]], data:Seq[Sym[_]], ens:Seq[Set[Bit]]) = {
    val bufferPort = lhs.port.bufferPort
    val muxPort = lhs.port.muxPort
    val broadcast = lhs.port.broadcast
    val castgroup = lhs.port.castgroup
    stateStruct(lhs, mem.asMem.A){ field => 
      val name = field.map { _._1 }
      var body = (bank, ofs) match {
        case (Some(bank), Some(ofs)) =>
          src"BankedWrite()" + 
          src".bank(${assertOne(bank)})" + 
          src".offset(${assertOne(ofs)})"
        case _ => 
          src"MemWrite()"
      }
      body += 
        src".setMem(${Lhs(mem,name)})" + 
        src".en(${assertOne(ens)})" + 
        src".data(${Lhs(assertOne(data),name)})" + 
        src".port($bufferPort)" + 
        src".muxPort($muxPort)" +
        src".broadcast($broadcast)" + 
        src".castgroup($castgroup)" + 
        (if (lhs.sym.isInnerReduceOp) ".isInnerReduceOp(true)" else "")
      body
    }
  }

  def genOp(lhs:Lhs, op:Option[String]=None, inputs:Option[Seq[Any]]=None) = {
    val rhs = lhs.sym.op.get
    val opStr = op.getOrElse(rhs.getClass.getSimpleName)
    val ins = inputs.getOrElse(rhs.productIterator.toList).map { in => src"$in"}.mkString(",")
    state(lhs) { 
      src"""OpDef(op=$opStr).addInput($ins)""" + 
      src""".tp(${lhs.sym.tp})""" + 
      (if (lhs.sym.isInnerReduceOp) ".isInnerReduceOp(true)" else "")
    }
  }

  implicit class OptionHelper[T](opt:Option[T]) {
    def ms(func: T => String) = opt.fold("")(func)
  }

}
