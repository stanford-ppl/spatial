package emul

import scala.reflect.ClassTag

class LineBuffer[T:ClassTag](
  name:     String,
  dims:     Seq[Int],
  banks:    Seq[Int],
  data:     Array[Array[T]],
  invalid:  T,
  depth:    Int,
  stride:   Int
) {
  private var bufferRow: Int = 0
  private var readRow: Int = stride
  private var wrCounter: Int = 0
  private var lastWrRow: Int = 0
  private val fullRows: Int = dims.head + (depth-1)*stride

  private def posMod(n: Int, d: Int): Int = ((n % d)+d) % d 
  def swap(): Unit = {
    bufferRow = posMod(bufferRow - stride, fullRows)
    readRow = posMod(readRow - stride, fullRows)
    wrCounter = 0
  }

  def flattenAddress(colbank: FixedPoint, ofs: FixedPoint): FixedPoint = {
     colbank + ofs.toInt * banks(1)
  }

  def apply(ctx: String, bank: Seq[Seq[FixedPoint]], ofs: Seq[FixedPoint], ens: Seq[Bool]): Array[T] = {
    Array.tabulate(bank.length){i =>
      val row = (bank(i).apply(0) + readRow) % fullRows
      val addr = s"Bank: ${row}, ${bank(i).apply(1)}; Ofs: ${ofs(i)}"
      OOB.readOrElse(name, addr, invalid, ens(i).value){
        if (ens(i).value) data.apply(row.toInt).apply(flattenAddress(bank(i).apply(1), ofs(i)).toInt) else invalid
      }
    }
  }

  def update(ctx: String, row: FixedPoint, ens: Seq[Bool], elems: Seq[T]): Unit = {
    val bank0 = posMod((stride-1-row.toInt) + bufferRow, fullRows)
    if (bank0 != lastWrRow) wrCounter = 0
    lastWrRow = bank0
    var numWritten = 0
    elems.indices.foreach{i => 
      val bank1 = posMod((wrCounter + i), banks(1))
      val ofs = (wrCounter + i) / banks(1)
      val addr = s"Bank: $bank0, $bank1; Ofs: $ofs "
      OOB.writeOrElse(name, addr, elems(i), ens(i).value){
        if (ens(i).value) {
          numWritten = numWritten + 1
          data.apply(bank0.toInt).update(flattenAddress(FixedPoint(bank1, row.fmt),FixedPoint(ofs, row.fmt)).toInt,elems(i))
        }
      }
    }
    wrCounter = wrCounter + numWritten
  }

  def initMem(size: Int, zero: T): Unit = {}
}



