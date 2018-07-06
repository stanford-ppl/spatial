package emul

import scala.reflect.ClassTag

class ShiftableMemory[T:ClassTag](
  name:     String,
  dims:     Seq[Int],
  data:     Array[T],
  invalid:  T,
  saveInit: Boolean
) {
  private var needsInit: Boolean = true
  private val resetValue: Option[Array[T]] = if (saveInit) Some(data.clone()) else None

  def shiftInVec(ctx: String, inds: Seq[FixedPoint], axis: Int, elems: Array[T]): Unit = {
    val len = elems.length
    (dims(axis)-1 to 0 by -1).foreach{j =>
      val addrFrom = Seq.tabulate(dims.length){d => if (d != axis) inds(d) else FixedPoint.fromInt(j - len) }
      val addrTo   = Seq.tabulate(dims.length){d => if (d != axis) inds(d) else FixedPoint.fromInt(j) }
      val dat = if (j < len) elems(len-1 - j) else apply(ctx,addrFrom,TRUE)
      update(ctx,addrTo,TRUE,dat)
    }
  }

  def shiftIn(ctx: String, inds: Seq[FixedPoint], axis: Int, data: T): Unit = shiftInVec(ctx,inds,axis,Array(data))

  def reset(): Unit = resetValue match {
    case Some(rst) =>
      Array.copy(rst,0,data,0,rst.length)
    case None =>
      data.indices.foreach{i => data.update(i,invalid) }
  }

  def flattenAddress(indices: Seq[FixedPoint]): Int = {
    val strides = List.tabulate(dims.length){i => dims.drop(i+1).product }
    indices.zip(strides).map{case (i,s) => i.toInt*s}.sum
  }

  def apply(ctx: String, addr: Seq[FixedPoint], en: Bool): T = {
    val x   = s"Addr: ${addr.mkString(", ")}"
    OOB.readOrElse(name, x, invalid, en.value){
      if (en.value) data.apply(flattenAddress(addr)) else invalid
    }
  }

  def apply(ctx: String, addr: Seq[Seq[FixedPoint]], ens: Seq[Bool]): Array[T] = {
    Array.tabulate(addr.length){i =>
      val adr = addr(i)
      val en  = ens(i)
      apply(ctx, adr, en)
    }
  }

  def update(ctx: String, addr: Seq[FixedPoint], en: Bool, elem: T): Unit = {
    val x = s"Addr: ${addr.mkString(", ")}"
    OOB.writeOrElse(name, x, elem, en.value){
      if (en.value) data.update(flattenAddress(addr), elem)
    }
  }

  def update(ctx: String, addr: Seq[Seq[FixedPoint]], ens: Seq[Bool], elems: Seq[T]): Unit = {
    addr.zipWithIndex.foreach{case (adr, i) =>
      update(ctx, addr(i), ens(i), elems(i))
    }
  }


  def initMem(size: Int, zero: T): Unit = if (needsInit) {
    needsInit = false
  }
}
