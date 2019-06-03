package spatial.rewrites

import argon._
import spatial.node._
import spatial.lang._
import argon.node._
import emul.FixedPoint
import spatial.metadata.bounds._

trait LUTConstReadRewriteRules extends RewriteRules {
  private implicit val state:State = IR 

  IR.rewrites.add[LUTBankedRead[_,C forSome{ type C[_]}]]("ConstLutRead", {
    case (op@LUTBankedRead(Op(LUTNew(ConstSeq(dims), elems:Seq[Bits[a]])), ConstSeq(bank), ofs, enss), ctx, _) =>
      bank match {
        case Seq(addr:Seq[_]) if addr.exists { case addr:Seq[_] => true; case _ => false } =>
          val vecs = addr.collect { case addr:Seq[_] => addr.size }.toSet
          assert(vecs.size == 1, s"Different vector width for plasticine $addr")
          val vaddr = addr.map {
            case seq:Seq[_] => seq
            case e:Int => List.fill(vecs.head)(e)
          }.asInstanceOf[Seq[Seq[Int]]]
          val bank = vaddr.transpose
          val faddr = bank.map { addrs =>
            flattenND(addrs.asInstanceOf[Seq[Int]], dims.asInstanceOf[Seq[Int]])
          }
          val values = faddr.map { a => elems(a) match { case Const(c) => c; case c => throw new Exception(s"Non contant content in LUT $c") } }
          val b = boundVar[a](op.A.asInstanceOf[Bits[a]], state)
          b.asInstanceOf[Sym[a]].vecConst = values.toList
          Some(stage(VecAlloc[a](Seq(b))(op.A.asInstanceOf[Bits[a]], op.vT.asInstanceOf[Vec[a]])))
        case bank => 
          val faddr = bank.map { addrs =>
            flattenND(addrs.asInstanceOf[Seq[Int]], dims.asInstanceOf[Seq[Int]])
          }
          Some(stage(VecAlloc[a](faddr.map { a => elems(a)})(op.A.asInstanceOf[Bits[a]], op.vT.asInstanceOf[Vec[a]])))
      }
    case _ => None
  })

  def flattenND(inds:Seq[Int], dims:Seq[Int]):Int = {
    if (inds.size==1) return inds.head
    assert(inds.size == dims.size, s"flattenND inds=$inds dims=$dims have different size")
    val i::irest = inds
    val d::drest = dims
    i * drest.product + flattenND(irest, drest)
  }
}

object ConstSeq {
  def unapply(x:Seq[_]):Option[Seq[Any]] = {
    Some(x.map {
      case Const(x:FixedPoint) => x.value.toInt
      case Const(x) => x
      case VecConst(xs) => xs.map { case x:FixedPoint => x.value.toInt; case x => x }
      case ConstSeq(x) => x
      case _ => return None
    })
  }
}
