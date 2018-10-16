package spatial.codegen.scalagen

import argon._
import spatial.metadata.memory._
import spatial.lang._

import utils.multiLoopWithIndex

trait ScalaGenMemories extends ScalaGenBits {
  var globalMems: Boolean = false

  var lineBufSwappers: Map[Sym[_], Sym[_]] = Map()

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

  private def expandInits(mem: Sym[_], inits: Option[Seq[Sym[_]]]): Option[Seq[Sym[_]]] = {
    if (inits.isDefined) {
      val dims = mem.constDims
      val padding = mem.padding
      val pDims = dims.zip(padding).map{case (d:Int,p:Int) => d+p}
      val paddedInits = Seq.tabulate(pDims.product){i => 
        val coords = pDims.zipWithIndex.map{ case (b,j) =>
          i % (pDims.drop(j).product) / pDims.drop(j+1).product
        }
        if (coords.zip(dims).map{case(c:Int,d:Int) => c < d}.reduce{_&&_}) {
          val flatCoord = coords.zipWithIndex.map{ case (b,j) => 
            b * dims.drop(j+1).product
          }.sum
          inits.get(flatCoord)
        }
        else 
          inits.get.head
      }
      Some(paddedInits)
    }
    else None
  }

  def emitBankedInitMem(mem: Sym[_], rawinit: Option[Seq[Sym[_]]], tp: ExpType[_,_]): Unit = {
    val inst = mem.instance
    val dims = mem.constDims.zip(mem.padding).map{case(a,b) => a+b}
    val size = dims.product
    val init = expandInits(mem, rawinit)

    implicit val ctx: SrcCtx = mem.ctx

    val dimensions = dims.map(_.toString).mkString("Seq(", ",", ")")
    val numBanks = inst.nBanks.map(_.toString).mkString("Seq(", ",", ")")

    if (mem.isRegFile || mem.isLUT) {
      val name = s""""${mem.fullname}""""
      val data = init match {
        case Some(elems) if elems.length >= size => src"Array[$tp](${elems.take(size)})"
        case Some(elems) if elems.length < size  => src"Array[$tp]($elems) ++ Array.fill(${elems.size - size}){ ${invalid(tp)} }"
        case None        => src"Array.fill($size){ ${invalid(tp)} }"
      }
      emitMemObject(mem) {
        open(src"object $mem extends ShiftableMemory[$tp](")
          emit(src"name = $name,")
          emit(src"dims = $dimensions,")
          emit(src"data = $data,")
          emit(src"invalid = ${invalid(tp)},")
          emit(src"saveInit = ${init.isDefined}")
        close(")")
      }
    }
    else {
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
            val bankAddr = inst.bankSelects(mem, is)
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

      emitMemObject(mem){
        val name = s""""${mem.fullname}""""
        open(src"object $mem extends BankedMemory(")
          emit(src"name  = $name,")
          emit(src"dims  = $dimensions,")
          emit(src"banks = $numBanks,")
          emit(src"data  = $data,")
          emit(src"invalid = ${invalid(tp)},")
          emit(src"saveInit = ${init.isDefined}")
        close(")")
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

  def emitVectorLoad[T:Type](lhs: Sym[_], mem: Sym[_], addr: Seq[Seq[Idx]], ens: Seq[Set[Bit]]): Unit = {
    val fullAddr = addr.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
    val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
    val ctx = s""""${lhs.ctx}""""
    emit(src"val $lhs = $mem.apply($ctx, $fullAddr, $enables)")
  }

  def emitBankedStore[T:Type](lhs: Sym[_], mem: Sym[_], data: Seq[Sym[T]], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]): Unit = {
    val bankAddr = bank.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
    val ofsAddr  = ofs.map(quote).mkString("Seq(", ",", ")")
    val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
    val datas    = data.map(quote).mkString("Seq(", ",", ")")
    val ctx = s""""${lhs.ctx}""""
    emit(src"val $lhs = $mem.update($ctx, $bankAddr, $ofsAddr, $enables, $datas)")
  }

  def emitVectorStore[T:Type](lhs: Sym[_], mem: Sym[_], data: Seq[Sym[T]], addr: Seq[Seq[Idx]], ens: Seq[Set[Bit]]): Unit = {
    val fullAddr = addr.map(_.map(quote).mkString("Seq(", ",", ")")).mkString("Seq(", ",", ")")
    val enables  = ens.map(en => and(en)).mkString("Seq(", ",", ")")
    val datas    = data.map(quote).mkString("Seq(", ",", ")")
    val ctx = s""""${lhs.ctx}""""
    emit(src"val $lhs = $mem.update($ctx, $fullAddr, $enables, $datas)")
  }

}
