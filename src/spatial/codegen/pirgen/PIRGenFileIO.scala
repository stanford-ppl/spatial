package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenFileIO extends PIRCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:CSVFile    => "java.io.File"
    case _:BinaryFile => "String"
    case _ => super.remap(tp)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case OpenCSVFile(filename, write) =>
      open(src"val $lhs = {")
        emit(src"val file = new java.io.File($filename)")
        open(src"if ($write) { // Will write to file")
          emit(src"val writer = new java.io.PrintWriter(file)")
          emit(src"""writer.print("")""")
        close("}")
        emit(src"file")
      close("}")

    case CloseCSVFile(file) => // Nothing for now?

    case ReadTokens(file, delim) =>
      open(src"val $lhs = {")
        emit(src"val scanner = new java.util.Scanner($file)")
        emit(src"val tokens = new scala.collection.mutable.ArrayBuffer[String]() ")
        emit(src"""scanner.useDelimiter("\\s*" + $delim + "\\s*|\\s*\n\\s*")""")
        open(src"while (scanner.hasNext) {")
          emit(src"tokens += scanner.next.trim")
        close("}")
        emit(src"scanner.close()")
        emit(src"tokens.toArray")
      close("}")

    case WriteTokens(file, delim, len, token) =>
      open(src"val $lhs = {")
        emit(src"val writer = new java.io.PrintWriter(new java.io.FileOutputStream($file, true /*append*/))")
        open(src"(0 until $len.toInt).foreach{${token.input} => ")
          emit(src"writer.write($delim)")
          gen(token)
          emit(src"writer.write(${token.result})")
        close("}")
        emit(src"writer.close()")
      close("}")


    case OpenBinaryFile(filename, write) => emit(src"val $lhs = $filename")

    case op @ ReadBinaryFile(file) =>
      open(src"val $lhs = {")
        emit(src"val filepath = java.nio.file.Paths.get($file)")
        emit(src"val buffer = java.nio.file.Files.readAllBytes(filepath)")
        emit(src"val bb = java.nio.ByteBuffer.wrap(buffer)")
        emit(src"bb.order(java.nio.ByteOrder.nativeOrder)")
        emit(src"val array = bb.array")
        val bytes = Math.ceil(op.A.nbits.toDouble / 8).toInt

        open(s"array.sliding($bytes,$bytes).toArray.map{x => ")
        op.A match {
          case FixPtType(s,i,f) => emit(src"FixedPoint.fromByteArray(x, FixFormat($s,$i,$f))")
          case FltPtType(g,e)   => emit(src"FloatPoint.fromByteArray(x, FltFormat(${g-1},$e))")
          case tp => throw new Exception(s"Reading binary files of type $tp is not yet supported")
        }
        close("}")
      close("}")

    case op @ WriteBinaryFile(file, len, value) =>
      open(src"val $lhs = {")
        emit(src"var ${value.input} = FixedPoint.fromInt(0)")
        emit(src"val stream = new java.io.DataOutputStream(new java.io.FileOutputStream($file))")
        open(src"while (${value.input} < $len) {")
          gen(value)
          emit(src"val value = ${value.result}")
          op.A match {
            case FixPtType(_,_,_) => emit(src"value.toByteArray.foreach{byte => stream.writeByte(byte) }")
            case FltPtType(_,_)   => emit(src"value.toByteArray.foreach{byte => stream.writeByte(byte) }")
            case tp => throw new Exception(s"Binary file reading not yet supported for type $tp")
          }
          emit(src"${value.input} += 1")
        close("}")
        emit(src"stream.close()")
      close("}")

    case CloseBinaryFile(file) => // Anything for this?

    case _ => super.gen(lhs, rhs)
  }

}
