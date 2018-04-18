package spatial.codegen.pirgen

import argon._

import spatial.data._
import spatial.lang._
import spatial.util._

import utils.multiLoopWithIndex

trait PIRGenMemories extends PIRGenBits {
  var globalMems: Boolean = false

  override def emitPreMain(): Unit = {
    emit(src"OOB.open()")
    super.emitPreMain()
  }

  override def emitPostMain(): Unit = {
    emit(src"OOB.close()")
    super.emitPostMain()
  }

  def emitMem(lhs: Sym[_], x: String): Unit = if (globalMems) emit(s"if ($lhs == null) $x") else emit("val " + x)

  def flattenAddress(dims: Seq[Idx], indices: Seq[Idx], ofs: Option[Idx]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*") }
    indices.zip(strides).map{case (i,s) => src"$i*$s" }.mkString(" + ") + ofs.map{o => src" + $o"}.getOrElse("")
  }

  def flattenAddress(dims: Seq[Idx], indices: Seq[Idx]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*") }
    indices.zip(strides).map{case (i,s) => src"$i*$s"}.mkString(" + ")
  }

  private def oob(tp: Type[_], mem: Sym[_], lhs: Sym[_], inds: Seq[Sym[_]], pre: String, post: String, isRead: Boolean)(lines: => Unit): Unit = {
    val name = mem.name.getOrElse(mem.toString)
    val addr = if (inds.isEmpty && pre == "" && post == "") "err.getMessage"
    else "\"" + pre + "\" + " + "s\"\"\"${" + inds.map(quote).map(_ + ".toString").mkString(" + \", \" + ") + "}\"\"\" + \"" + post + "\""

    val op = if (isRead) "read" else "write"

    open(src"try {")
      lines
    close("}")
    open(src"catch {case err: java.lang.ArrayIndexOutOfBoundsException => ")
      emit(s"""System.out.println("[warn] ${lhs.ctx} Memory $name: Out of bounds $op at address " + $addr)""")
      if (isRead) emit(src"${invalid(tp)}")
    close("}")
  }

  def oobApply(tp: Type[_], mem: Sym[_], lhs: Sym[_], inds: Seq[Sym[_]], pre: String = "", post: String = "")(lines: => Unit): Unit = {
    oob(tp, mem, lhs, inds, pre, post, isRead = true)(lines)
  }

  def oobUpdate(tp: Type[_], mem: Sym[_], lhs: Sym[_], inds: Seq[Sym[_]], pre: String = "", post: String = "")(lines: => Unit): Unit = {
    oob(tp, mem, lhs, inds, pre, post, isRead = false)(lines)
  }


  def emitMemObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(kernel(lhs)){
      emitHeader()
      contents
      emitFooter()
    }
  }

  def emitBankedInitMem(mem: Sym[_], init: Option[Seq[Sym[_]]], tp: ExpType[_,_]): Unit = {
    val inst = memInfo(mem)
    val dims = constDimsOf(mem)
    implicit val ctx: SrcCtx = mem.ctx

    val data = init match {
      case Some(elems) =>
        val nBanks = inst.nBanks.product
        val bankDepth = Math.ceil(dims.product.toDouble / nBanks).toInt

        dbg(s"Generating initialized memory: ${stm(mem)}")
        dbg(s"# Banks: $nBanks")
        dbg(s"Bank Depth: $bankDepth")

        // Import an implicit lexicographic ordering for the bank addresses (allows us to group and sort by bank addr)
        import scala.math.Ordering.Implicits._

        val banks = multiLoopWithIndex(dims).map { case (is, i) =>
          val bankAddr = inst.bankSelects(is)
          val ofs = inst.bankOffset(mem, is)
          dbg("  " + is.mkString(", ") + s" ($i): elem:${elems(i)}, bankAddr:$bankAddr, ofs:$ofs" )

          (elems(i), bankAddr, ofs)
        }.toArray.groupBy(_._2).toList.sortBy(_._1).map{case (_, vals) =>
          val elems = (0 until bankDepth).meta.map{i => vals.find(_._3 == i).map(_._1).getOrElse(invalid(tp)) }
          src"Array[$tp]($elems)"
        }
        src"""Array[Array[$tp]](${banks.mkString(",\n")})"""
      case None =>
        val banks = inst.totalBanks
        val bankDepth = Math.ceil(dims.product.toDouble / banks).toInt
        src"""Array.fill($banks){ Array.fill($bankDepth)(${invalid(tp)}) }"""
    }
    val dimensions = dims.map(_.toString).mkString("Seq(", ",", ")")
    val numBanks = inst.nBanks.map(_.toString).mkString("Seq(", ",", ")")

    if (mem.isRegFile) {
      // HACK: Stage, then generate, the banking and offset addresses for the regfile on the fly
      val addr = Seq.fill(rankOf(mem)){ bound[I32] }
      val bankAddrFunc = stageBlock{
        val bank = inst.bankSelects(addr)
        implicit val vT: Type[Vec[I32]] = Vec.bits[I32](bank.length)
        Vec.fromSeq(bank)
      }
      val offsetFunc = stageBlock{ inst.bankOffset(mem,addr) }

      val name = s""""${mem.fullname}""""
      emitMemObject(mem) {
        open(src"object $mem extends ShiftableMemory($name, $dimensions, $numBanks, $data, ${invalid(tp)}, saveInit = ${init.isDefined}, {")
        emit(src"""case Seq(${addr.mkString(",")}) => """)
        bankAddrFunc.stms.foreach(visit)
        emit(src"${bankAddrFunc.result}")
        close("},")
        open("{")
        emit(src"""case Seq(${addr.mkString(",")}) => """)
        offsetFunc.stms.foreach(visit)
        emit(src"${offsetFunc.result}")
        close("})")
      }
    }
    else {
      emitMemObject(mem){
        val name = s""""${mem.fullname}""""
        emit(src"object $mem extends BankedMemory($name, $dimensions, $numBanks, $data, ${invalid(tp)}, saveInit = ${init.isDefined})")
      }
    }
  }

  def emitBankedLoad[T:Type](lhs: Sym[_], mem: Sym[_], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]): Unit = {
    val bankAddr = bank.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
    val ofsAddr  = ofs.map(quote).mkString("Seq(", ",", ")")
    val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
    val ctx = s""""${lhs.ctx}""""
    emit(src"val $lhs = $mem.apply($ctx, $bankAddr, $ofsAddr, $enables)")
  }

  def emitBankedStore[T:Type](lhs: Sym[_], mem: Sym[_], data: Seq[Sym[T]], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]): Unit = {
    val bankAddr = bank.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
    val ofsAddr  = ofs.map(quote).mkString("Seq(", ",", ")")
    val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
    val datas    = data.map(quote).mkString("Seq(", ",", ")")
    val ctx = s""""${lhs.ctx}""""
    emit(src"val $lhs = $mem.update($ctx, $bankAddr, $ofsAddr, $enables, $datas)")
  }

}
