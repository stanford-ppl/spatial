package emul

import scala.language.implicitConversions

object implicits {
  implicit def fixedPointToInt(x: FixedPoint): Int = x.toInt
  implicit def intToFixedPoint(x: Int): FixedPoint = FixedPoint.fromInt(x)
  implicit def boolToBoolean(x: Bool): Boolean = x.value
  implicit def booleanToBool(x: Boolean): Bool = Bool(x)

  implicit class BoolArrayOps(x: Array[Bool]) {
    def toStr: String = "0b" + x.sliding(4,4).map{nibble =>
      nibble.map{b => b.toStr}.reverse.mkString("")
    }.toList.reverse.mkString(",")

    def toFmtStr(fmt: FltFormat): String = {
      val sign = x.last
      val s = if (sign.value) "1" else "0"
      val exp = x.slice(fmt.sbits, fmt.sbits+fmt.ebits)
      val e = exp.toStr.drop(2)
      val sig = x.slice(0, fmt.sbits)
      val g = sig.toStr.drop(2)
      s"$s|$e|$g"
    }
  }

  implicit class StringArrayOps(x: String) {
    def toStr: String = x.reverse.sliding(4,4).map{nibble =>
      nibble.reverse.mkString("")
    }.toList.reverse.mkString(",")
  }

  implicit class ByteArrayOps(x: Array[Byte]) {
    def toStr: String = "0b" + x.reverse.flatMap{byte =>
      val big = List.tabulate(4){i => if ((byte & (1 << (i+4))) > 0) "1" else "0" }.reverse.mkString("")
      val ltl = List.tabulate(4){i => if ((byte & (1 << i)) > 0) "1" else "0" }.reverse.mkString("")
      List(big,ltl)
    }.mkString(",")
  }
}