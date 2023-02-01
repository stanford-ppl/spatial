package spatial.util

import argon._
import forge.tags.{api, ref}
import spatial.lang._

case class MapStructType[T](structFields: Seq[(T, Bits[_])], errorOnMismatch: Boolean = true, typeName: Option[String] = None) {

  private val internalStructFields: Seq[(String, Bits[_])] = if (structFields.nonEmpty) {
    structFields.map {
      case (key, bits) => key.toString -> bits
    }
  } else {
    Seq("placeholder" -> Bit(false))
  }
  @api def apply(entries: Map[T, _ <: Bits[_]]): MapStruct = {
    val strEntries = entries.map {
      case (k, v) => k.toString -> v
    } ++ Map("placeholder" -> Bit(false))

    val fields = (internalStructFields map {
      case (name, _) =>
        name -> strEntries(name)
    })
    Struct[MapStruct](fields:_*)
  }

  private val typeString = typeName.getOrElse("Unnamed")

  override lazy val toString: String = {
    s"$typeString(${structFields.map {
      case (key, bEV) => s"${key.toString}: ${bEV.tp.toString}"
    }.mkString(", ")})"
  }
  @ref class MapStruct extends Struct[MapStruct] with Bits[MapStruct] with Ref[Map[T, Any], MapStruct] {
    override val box = implicitly[MapStruct <:< (Bits[MapStruct] with Struct[MapStruct])]

    def fields: Seq[(String, ExpType[_, _])] = internalStructFields.map {
      case (key, value) => key -> value.asInstanceOf[Type[_]]
    }

    @api override def zero: MapStruct = Struct[MapStruct]((internalStructFields.map {
      case (key, value) => key -> value.zero.asInstanceOf[Sym[_]]
    }):_*)

    @api override def nbits: Int = internalStructFields.map(_._2.nbits).sum

    @api override def one: MapStruct =
      Struct[MapStruct]((internalStructFields.map {
        case (key, value) => key -> value.one.asInstanceOf[Sym[_]]
      }): _*)

    @api override def random(max: Option[MapStruct]): MapStruct = max match {
      case None =>
        Struct[MapStruct]((internalStructFields.map {
          case (key, value) => key -> value.random(None).asInstanceOf[Sym[_]]
        }): _*)
      case Some(vecStruct: MapStruct) =>
        val data = (vecStruct.unpack.map {
          case (k, v) => k.toString -> v.asInstanceOf[SomeBits]
        }).toMap[String, SomeBits]
        Struct[MapStruct]((internalStructFields.map {
          case (key, value) =>
            val rv = value.asInstanceOf[Bits[SomeBits]].random(data.get(key))
            key -> rv.asInstanceOf[Sym[_]]
        }):_*)
    }

    type SomeBits = T forSome {type T <: Bits[T]}
    @api def unpack: Seq[(T, Bits[_])] = {
      if (structFields.isEmpty) {
        Seq.empty
      } else {
        structFields.map {
          case (key, bits: SomeBits) =>
            implicit def tEV: Type[SomeBits] = bits.asInstanceOf[Type[SomeBits]]
            key -> field[SomeBits](key.toString)
        }
      }
    }
  }

  type VFIFO = FIFO[MapStruct]

  @api def VFIFO(size: I32): VFIFO = {
    FIFO[MapStruct](size)
  }
}

