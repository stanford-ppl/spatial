package spatial.codegen.cppgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._
import spatial.utils._

import scala.collection.mutable.ListBuffer

trait CppGenHostTransfer extends CppGenSRAM  {

  override protected def spatialNeedsFPType(tp: Type[_]): Boolean = tp match { // FIXME: Why doesn't overriding needsFPType work here?!?!
      case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
      case IntType()  => false
      case LongType() => false
      case FloatType() => true
      case DoubleType() => true
      case HalfType() => true
      case _ => super.needsFPType(tp)
  }

  override protected def name(s: Dyn[_]): String = s match {
  	case Def(SetArg(reg:Sym[_],_)) => s"${s}_set${reg.id}"
    case Def(GetArg(reg:Sym[_]))   => s"${s}_get${reg.id}"
    case Def(SetMem(_,_))          => s"${s}_setMem"
    case Def(GetMem(_,_))          => s"${s}_getMem"
    case _ => super.name(s)
  } 

  case class JSONEntry(name: String, idx: Int, size: String) {
    def jsonFields = List(
        s""""name" : "$name"""",
        s""""idx"  : $idx""",
        s""""size" : "$size""""
      )
  }

  val jsonData = ListBuffer[JSONEntry]()

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SetArg(reg, v) => 
      reg.tp.typeArguments.head match {
        case FixPtType(s,d,f) => 
          if (f != 0) {
            emit(src"c1->setArg(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, (int64_t)($v * ((int64_t)1 << $f)), ${isHostIO(reg)}); // $reg")
            emit(src"$reg = $v;")
          } else {
            emit(src"c1->setArg(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, $v, ${isHostIO(reg)});")
            emit(src"$reg = $v;")
          }
        case FltPtType(g,e) =>         
          emit(src"int64_t ${v}_raw;")
          emit(src"memcpy(&${v}_raw, &${v}, sizeof(${v}));")
          emit(src"${v}_raw = ${v}_raw & ((int64_t) 0 | (int64_t) pow(2,${g+e}) - 1);")
          emit(src"c1->setArg(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, ${v}_raw, ${isHostIO(reg)}); // $reg")
          emit(src"$reg = $v;")
        case _ => 
            emit(src"c1->setArg(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, $v, ${isHostIO(reg)}); // $reg")
            emit(src"$reg = $v;")
      }
    case GetArg(reg)    => 
      val bitWidth = reg.tp.typeArguments.head match {case FixPtType(s,d,f) => d+f; case FltPtType(g,e) => g+e; case _ => 32}
      val bigArg = if (bitWidth > 32 & bitWidth <= 64) "64" else ""
      val get_string = reg match {
        case Def(ArgInNew(_)) => src"c1->getArgIn(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, ${isHostIO(reg)})" 
        case _ => src"c1->getArg${bigArg}(${reg.name.getOrElse(quote(reg)).toUpperCase}_arg, ${isHostIO(reg)})"
      }
      reg.tp.typeArguments.head match {
        case FixPtType(s,d,f) => 
          emit(src"int64_t ${lhs}_tmp = ${get_string};")            
          emit(src"bool ${lhs}_sgned = $s & ((${lhs}_tmp & ((int64_t)1 << ${d+f-1})) > 0); // Determine sign")
          emit(src"if (${lhs}_sgned) ${lhs}_tmp = ${lhs}_tmp | ~(((int64_t)1 << ${d+f})-1); // Sign-extend if necessary")
          emit(src"${lhs.tp} ${lhs} = (${lhs.tp}) ${lhs}_tmp / ((int64_t)1 << $f);")            
        case FltPtType(g,e) => 
          emit(src"int64_t ${lhs}_tmp = ${get_string};")            
          emit(src"${lhs.tp} ${lhs};")
          emit(src"memcpy(&${lhs}, &${lhs}_tmp, sizeof(${lhs}));")
        case _ => 
          emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${get_string};")
      }

      
    case SetMem(dram, data) => 
      val rawtp = remapIntType(dram.tp.typeArguments.head)
      if (spatialNeedsFPType(dram.tp.typeArguments.head)) {
        dram.tp.typeArguments.head match { 
          case FixPtType(s,d,f) => 
            emit(src"vector<${rawtp}>* ${dram}_rawified = new vector<${rawtp}>((*${data}).size());")
            open(src"for (int ${dram}_rawified_i = 0; ${dram}_rawified_i < (*${data}).size(); ${dram}_rawified_i++) {")
              emit(src"(*${dram}_rawified)[${dram}_rawified_i] = (${rawtp}) ((*${data})[${dram}_rawified_i] * ((${rawtp})1 << $f));")
            close("}")
            emit(src"c1->memcpy($dram, &(*${dram}_rawified)[0], (*${dram}_rawified).size() * sizeof(${rawtp}));")
          case _ => emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${rawtp}));")
        }
      } else {
        emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${rawtp}));")
      }

      val size = stagedDimsOf(dram).map { d => d match {
        case Exact(c) => c.toInt
        case _ => 0
      }}.reduce {_*_}

      val name = getDef(data) match {
        case Some(NumpyArray(str)) => str
        case _ => "null"
      }

      jsonData.append(JSONEntry(name = s"$name", idx = argMapping(dram)._2, size = s"$size"))

      val effects = effectsOf(lhs)

    case GetMem(dram, data) => 
      val rawtp = remapIntType(dram.tp.typeArguments.head)
      if (spatialNeedsFPType(dram.tp.typeArguments.head)) {
        dram.tp.typeArguments.head match { 
          case FixPtType(s,d,f) => 
            emit(src"vector<${rawtp}>* ${data}_rawified = new vector<${rawtp}>((*${data}).size());")
            emit(src"c1->memcpy(&(*${data}_rawified)[0], $dram, (*${data}_rawified).size() * sizeof(${rawtp}));")
            open(src"for (int ${data}_i = 0; ${data}_i < (*${data}).size(); ${data}_i++) {")
              emit(src"${rawtp} ${data}_tmp = (*${data}_rawified)[${data}_i];")
              emit(src"(*${data})[${data}_i] = (double) ${data}_tmp / ((${rawtp})1 << $f);")
            close("}")
          case _ => emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${rawtp}));")
        }
      } else {
        emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${rawtp}));")
      }

      val size = stagedDimsOf(dram).map { d => d match {
        case Exact(c) => c.toInt
        case _ => 0
      }}.reduce {_*_}

      val name = getDef(data) match {
        case Some(NumpyArray(str)) => str
        case _ => "null"
      }

      jsonData.append(JSONEntry(name = s"$name", idx = argMapping(dram)._2, size = s"$size"))


    case _ => super.emitNode(lhs, rhs)
  }

  override protected def emitFileFooter() {
    withStream(getStream("argmap","json")) {
      val jsonOrdered = jsonData.distinct.sortBy { _.idx }
      val maps = jsonOrdered.map { j =>
          "{ " + j.jsonFields.mkString(",") + " }"
      }.mkString(",\n")
      emit(maps)
    }
    super.emitFileFooter()
  }

}
