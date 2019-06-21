package spatial.codegen.tsthgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.codegen.cppgen._

trait TungstenHostGenArray extends TungstenHostGenCommon with CppGenArray with TungstenHostGenInterface {
  override def emitHeader = {
    super.emitHeader

    genIO {
      emit("""
typedef __int128 int128_t;
""")
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SimpleStruct(st) => 
      // val struct = st.map{case (name, data) => src"${name}${data.tp}".replaceAll("[<|>]","")}.mkString("")
      val struct = src"${lhs.tp}".replaceAll("[<|>]","")
      // Add to struct header if not there already
      if (!struct_list.contains(struct)) {
        struct_list = struct_list :+ struct
        genIO {
          open(src"struct ${struct} {")
            st.foreach{f => emit(src"${asIntType(f._2.tp)}${ptr(f._2.tp)} ${f._1}; // ${f._2.tp}")}
            open(src"${struct}(${st.map{f => src"${f._2.tp}${ptr(f._2.tp)} ${f._1}_in"}.mkString(",")}){ /* Normal Constructor */")
              st.foreach{f => emit(src"set${f._1}(${ptr(f._2.tp)}${f._1}_in);")}
            close("}")
            emit(src"${struct}(){} /* For creating empty array */")
            open(src"std::string toString(){")
              val all = st.map{f => src""" "${f._1}: " + std::to_string(${ptr(f._2.tp)}${toApproxFix(src"${f._1}", f._2.tp)})"""}.mkString("+ \", \" + ")
              emit(src"return $all;")
            close("}")
            st.foreach{f => emit(src"void set${f._1}(${f._2.tp} x){ this->${f._1} = ${amp(f._2.tp)}${toTrueFix("x",f._2.tp)}; }")}

          try {
            val rawtp = asIntType(lhs.tp)
            var position = 0
            open(src"$rawtp toRaw() { /* For compacting struct into one int */")
              emit(src"$rawtp result = 0;")
              st.foreach{f => 
                val field = f._1
                val t = f._2
                emit(src"result = result | (($rawtp) (this->$field) << $position); ")
                position = position + bitWidth(t.tp)
              }
              emit(src"return result;")
            close("}")
            position = 0
            open(src"${struct}(int128_t bits){ /* Constructor from raw bits */")
              st.foreach{f => emit(src"set${f._1}((${f._2.tp}) (bits >> $position));"); position = position + bitWidth(f._2.tp)}
            close("}")
            close(" ")
          } catch { case _:Throwable => }

          close("} __attribute__((packed));")
          // emit(src"typedef $struct ${lhs.tp};")

        }
      }
      val fields = st.zipWithIndex.map{case (f,i) => src"${f._2}"}
      emit(src"${struct} $lhs = ${struct}(${fields.mkString(",")});")

    case _ => super.gen(lhs, rhs)
  }

}
