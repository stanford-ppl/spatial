package emul

import scala.reflect.ClassTag

class BankedMemory[T:ClassTag](
  name:     String,
  dims:     Seq[Int],
  banks:    Seq[Int],
  data:     Array[Array[T]],
  invalid:  T,
  saveInit: Boolean
) {
  private var needsInit: Boolean = true
  private val resetValue: Option[Array[Array[T]]] = if (saveInit) Some(data.map(_.clone())) else None

  def reset(): Unit = resetValue match {
    case Some(rst) =>
      data.indices.foreach{i => Array.copy(rst(i),0,data(i),0,rst(i).length) }
    case None =>
      data.indices.foreach{i => data(i).indices.foreach{j => data.apply(i).update(j,invalid) }}
  }

  def flattenAddress(dims: Seq[Int], indices: Seq[FixedPoint]): Int = {
    val strides = List.tabulate(dims.length){i => dims.drop(i+1).product }
    indices.zip(strides).map{case (i,s) => i.toInt*s}.sum
  }

  def apply(ctx: String, bank: Seq[Seq[FixedPoint]], ofs: Seq[FixedPoint], ens: Seq[Bool]): Array[T] = {
    Array.tabulate(bank.length){i =>
      val addr = s"Bank: ${bank(i).mkString(", ")}; Ofs: ${ofs(i)}"
      OOB.readOrElse(name, addr, invalid, ens(i).value){
        if (ens(i).value) data.apply(flattenAddress(banks,bank(i))).apply(ofs(i).toInt) else invalid
      }
    }
  }

  def update(ctx: String, bank: Seq[Seq[FixedPoint]], ofs: Seq[FixedPoint], ens: Seq[Bool], elems: Seq[T]): Unit = {
    bank.indices.foreach{i =>
      val addr = s"Bank: ${bank(i).mkString(", ")}; Ofs: ${ofs(i)}"
      OOB.writeOrElse(name, addr, elems(i), ens(i).value){
        if (ens(i).value) data.apply(flattenAddress(banks,bank(i))).update(ofs(i).toInt,elems(i))
      }
    }
  }

  def initMem(size: Int, zero: T): Unit = if (needsInit) {
    needsInit = false
  }
}



