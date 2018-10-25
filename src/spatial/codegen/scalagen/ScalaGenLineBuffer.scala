package spatial.codegen.scalagen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._


import utils.implicits.collections._

trait ScalaGenLineBuffer extends ScalaGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: LineBuffer[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@LineBufferNew(rows, cols, stride) => 
      val inst = lhs.instance
      val banks = inst.nBanks.map(_.toString).mkString("Seq[Int](", ",", ")")
      val volume = rows + (inst.depth-1)*stride * cols
      val bankDepth = volume / banks.product
      val data = src"""Array.fill(${rows + (inst.depth-1)*stride}){ Array.fill($cols){ ${invalid(lhs.tp.typeArgs.head)} }}"""
      emitMemObject(lhs){
        val name = s""""${lhs.fullname}""""
        open(src"object $lhs extends LineBuffer(")
          emit(src"name  = $name,")
          emit(src"dims  = Seq[Int]($rows, $cols),")
          emit(src"banks = $banks,")
          emit(src"data  = $data,")
          emit(src"invalid = ${invalid(lhs.tp.typeArgs.head)},")
          emit(src"depth = ${inst.depth},")
          emit(src"stride = $stride")
        close(")")
      }
      connectBufSignals(lhs)
    case op@LineBufferBankedEnq(lb, data, row, ens) => 
      val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
      val datas    = data.map(quote).mkString("Seq(", ",", ")")
      val ctx = s""""${lhs.ctx}""""
      emit(src"val $lhs = $lb.update($ctx, $row, $enables, $datas)")
    case op@LineBufferBankedRead(lb, bank, ofs, ens) => 
      val bankAddr = bank.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
      val ofsAddr  = ofs.map(quote).mkString("Seq(", ",", ")")
      val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
      val ctx = s""""${lhs.ctx}""""
      emit(src"val $lhs = $lb.apply($ctx, $bankAddr, $ofsAddr, $enables)")

    case _ => super.gen(lhs, rhs)
  }


  protected def bufferControlInfo(mem: Sym[_]): List[Sym[_]] = {
    val accesses = mem.accesses.filter(_.port.bufferPort.isDefined)
    if (accesses.nonEmpty) {
      val lca = if (accesses.size == 1) accesses.head.parent else LCA(accesses)
      if (lca.isParallel){ // Assume memory analysis chose buffering based on lockstep of different bodies within this parallel, and just use one
        val releventAccesses = accesses.toList.filter(_.ancestors.contains(lca.children.head)).toSet
        val logickingLca = LCA(releventAccesses)
        val (basePort, numPorts) = if (logickingLca.s.get.isInnerControl) (0,0) else LCAPortMatchup(releventAccesses.toList, logickingLca)
        val info = if (logickingLca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => logickingLca.children.toList(port).s.get }
        info.toList        
      } else {
        val (basePort, numPorts) = if (lca.s.get.isInnerControl) (0,0) else LCAPortMatchup(accesses.toList, lca)
        val info = if (lca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => lca.children.toList(port).s.get }
        info.toList        
      }
    } else {
      throw new Exception(s"Cannot create a buffer on $mem, which has no accesses")
    }

  }

  private def connectBufSignals(mem: Sym[_]): Unit = {
    val info = bufferControlInfo(mem)
    info.zipWithIndex.foreach{ case (node, port) => 
      if (port == 0) lineBufSwappers += (node -> {lineBufSwappers.getOrElse(node, Set()) ++ Set(mem)})
    }
  }

}
