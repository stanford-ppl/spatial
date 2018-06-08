package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.data._
import spatial.util._


trait CppGenFileIO extends CppGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case OpenBinaryFile(filename, isWr) =>
      val dir = if (isWr) "o" else "i"
      emit(src"""std::${dir}fstream ${lhs} ($filename, std::ios::binary);""")
      emit(src"""assert(${lhs}.good() && "File ${s"$filename".replace("\"","")} does not exist"); """)

    case op @ ReadBinaryFile(file) =>
      // Pull raw data out of file
      emit(src"${file}.seekg(0, std::ios::end);")
      emit(src"""std::ifstream::pos_type ${lhs}_pos = ${file}.tellg();""")
      emit(src"std::vector<char> ${lhs}_temp (${lhs}_pos); ")
      emit(src"${file}.seekg(0, std::ios::beg);")
      emit(src"${file}.read(&${lhs}_temp[0], ${lhs}_pos);")
      val chars = Math.ceil(op.A.nbits.toDouble / 8).toInt
      val rawtp = asIntType(op.A)
      // Place raw data in appropriately-sized vector with correct bit width
      emit(src"std::vector<${rawtp}>* ${lhs}_raw = new std::vector<${rawtp}>(${lhs}_temp.size()/${chars});")
      emit(src"std::memcpy((void*)&((*${lhs}_raw)[0]), &(${lhs}_temp[0]), ${lhs}_temp.size() * sizeof(char));")
      // Convert raw data into appropriate type
      emit(src"${lhs.tp}* ${lhs} = new ${lhs.tp}((*${lhs}_raw).size());")
      op.A match { 
        case FixPtType(s,d,f) => 
          open(src"for (int ${lhs}_i = 0; ${lhs}_i < (*${lhs}).size(); ${lhs}_i++) {")
            emit(src"(*${lhs})[${lhs}_i] = ${toApproxFix(src"(*${lhs}_raw)[${lhs}_i]", op.A)};")
          close("}")
        case _ => 
          emit(src"${lhs} = ${lhs}_raw;")
      }


    case op @ WriteBinaryFile(file, len, value) =>
      val i = value.input
      open(src"for (int ${i} = 0; ${i} < $len; ${i}++) {")
        // emit(src"${file}.write(reinterpret_cast<const char *>(&${value.result}_raw), sizeof(${op.A}));")
      val rawtp = asIntType(op.A)
      op.A match { 
        case FixPtType(s,d,f) => 
          visitBlock(value)
          emit(src"${rawtp} ${value.result}_raw = ${value.result} * ((${rawtp})1 << $f);")
          emit(src"${file}.write(reinterpret_cast<const char *>(&${value.result}_raw), sizeof(${rawtp}));")
        case _ => 
          emit(src"${file}.write(reinterpret_cast<const char *>(&${value.result}_raw), sizeof(${op.A}));")
      }

      close("}")
      
    case CloseBinaryFile(file) =>
      emit(src"$file.close();")


    case OpenCSVFile(filename, isWr) => 
      val dir = if (isWr) "o" else "i"
      emit(src"""std::${dir}fstream ${lhs}_file ($filename);""")
      emit(src"""assert(${lhs}_file.good() && "File ${s"$filename".replace("\"","")} does not exist"); """)
    case CloseCSVFile(file) =>
      emit(src"${file}_file.close();")
    case ReadTokens(file, delim) =>
      emit(src"std::vector<string>* ${lhs} = new std::vector<string>; ")
      open(src"if (${file}_file.is_open()) {")
        open(src"while ( ${file}_file.good() ) {")
          emit(src"string ${lhs}_line;")
          emit(src"""getline (${file}_file, ${lhs}_line);""")
          if (src"""${delim}""" == """"\n"""") {
              emit(src"""if (${lhs}_line != "") {${lhs}->push_back(${lhs}_line);}""")
          } else {
            emit(src"string ${lhs}_delim = ${delim};".replace("'","\""))
            emit(src"size_t ${lhs}_pos = 0;")
            open(src"while (${lhs}_line.find(${lhs}_delim) != std::string::npos | ${lhs}_line.length() > 0) {")
              open(src"if (${lhs}_line.find(${lhs}_delim) != std::string::npos) {")
                emit(src"${lhs}_pos = ${lhs}_line.find(${lhs}_delim);")
              closeopen("} else {")
                emit(src"${lhs}_pos = ${lhs}_line.length();")
              close("}")
              emit(src"string ${lhs}_token = ${lhs}_line.substr(0, ${lhs}_pos);")
              emit(src"${lhs}_line.erase(0, ${lhs}_pos + ${lhs}_delim.length());")
              emit(src"${lhs}->push_back(${lhs}_token);")
            close("}")
          }
        close("}")
      close("}")
      emit(src"${file}_file.clear();")
      emit(src"${file}_file.seekg(0, ${file}_file.beg);")

    case WriteTokens(file, delim, len, token) =>
      val i = token.input
      open(src"for (int ${i} = 0; ${i} < $len; ${i}++) {")
        open(src"if (${file}_file.is_open()) {")
          visitBlock(token)
          emit(src"${file}_file << ${token.result};")
          val chardelim = src"$delim".replace("\"","'").replace("string(","").dropRight(1)
          emit(src"""${file}_file << ${chardelim};""")
        close("}")
      close("}")

    case _ => super.gen(lhs, rhs)
  }


}
