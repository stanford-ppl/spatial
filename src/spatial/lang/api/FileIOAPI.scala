package spatial.lang.api

import argon._
import forge.tags._
import utils.io.files._

import spatial.node._

trait FileIOAPI { this: Implicits =>

  @rig def parseValue[A:Type](str: String): A = {
    def parse[B](s: String, tp: Type[B]): Sym[B] = {
      implicit val B: Type[B] = tp
      parseValue[B](s)
    }
    try {
      Type[A] match {
        case tp: Bit        => tp.from(str.trim)
        case tp: Fix[s,i,f] => tp.from(str.trim)
        case tp: Flt[g, e]  => tp.from(str.trim)
        case tp: Struct[_]  =>
          val entries = str.split(";").zip(tp.fields.map(_._2)).map{
            case (s,ftyp) => parse(s, ftyp)
          }
          implicit val tA: Struct[A] = tp.view[Struct]
          Struct[A](tp.fields.map(_._1).zip(entries):_*)
      }
    }
    catch {case t: Throwable =>
      error(ctx, s"Could not parse $str as a ${Type[A]}")
      error(ctx)
      throw t
    }
  }

  @api def loadConstants[A:Type](filename: String, delim: String = "\n"): Tensor1[A] = {
    val elems = loadCSVNow(filename,delim){x => parseValue[A](x) }
    Tensor1.apply(elems:_*)
  }

  @rig def openCSV(filename: Text, write: Boolean): CSVFile = stage(OpenCSVFile(filename, write))
  @rig def readTokens(file: CSVFile, delim: Text): Tensor1[Text] = stage(ReadTokens(file, delim))
  @rig def writeTokens(file: CSVFile, delim: Text, len: I32)(token: I32 => Text): Void = {
    val i = boundVar[I32]
    val func = stageLambda1(i){ token(i) }
    stage(WriteTokens(file, delim, len, func))
  }
  @rig def closeCSV(file: CSVFile): Void = stage(CloseCSVFile(file))

  /** Loads the CSV at `filename` as an @Array, using the supplied `delimeter` (comma by default). **/
  @api def loadCSV1D[T:Type](filename: Text, delim: Text = Text(","))(implicit cast: Cast[Text,T]): Tensor1[T] = {
    val file = openCSV(filename, write = false)
    val tokens = readTokens(file, delim)
    closeCSV(file)
    tokens.map{token => token.to[T] }
  }

  // TODO[4]: Reading matrices will not work if delim2 is not \n, so we need to have a read_tokens take multiple delimiters
  /** Loads the CSV at `filename`as a @Tensor2, using the supplied element delimeter and linebreaks across rows. **/
  @api def loadCSV2D[T:Type](filename: Text, delim1: Text = Text(","), delim2: Text = Text("\n"))(implicit cast: Cast[Text,T]): Tensor2[T] = {
    val file = openCSV(filename, write = false)
    val all_tokens = readTokens(file, delim1)
    val row_tokens = readTokens(file, delim2)
    closeCSV(file)
    val data = all_tokens.map{token => token.to[T] }
    Tensor2(data, row_tokens.length, all_tokens.length / row_tokens.length)
  }

  /** Writes the given Array to the file at `filename` using the given `delimiter`.
    * If no delimiter is given, defaults to comma.
    **/
  @api def writeCSV1D[T:Type](array: Tensor1[T], filename: Text, delim: Text = Text(","), format:Option[String]=None): Void = {
    val file = openCSV(filename, write = true)
    writeTokens(file, delim, array.length){i => array(i) match {
      case e:Fix[_,_,_] => e.toText(format)
      case e:Flt[_,_] => e.toText(format)
      case e => e.toText
    } }
    closeCSV(file)
  }

  /** Writes the given Tensor2 to the file at `filename` using the given element delimiter.
    * If no element delimiter is given, defaults to comma.
    **/
  @api def writeCSV2D[T:Type](matrix: Tensor2[T], filename: Text, delim1: Text = Text(","), delim2: Text = Text("\n"), format:Option[String]=None): Void = {
    val file = openCSV(filename, write = true)
    Foreach(0 until matrix.rows){ i =>
      writeTokens(file, delim1, matrix.cols){j => matrix(i,j) match {
        case e:Fix[_,_,_] => e.toText(format)
        case e:Flt[_,_] => e.toText(format)
        case e => e.toText
      } }
      writeTokens(file, delim2, 1){_ => Text("") }
    }
    closeCSV(file)
  }

  @rig def openBinary(filename: Text, write: Boolean): BinaryFile = stage(OpenBinaryFile(filename, write))
  @rig def readBinary[A:Num](file: BinaryFile, isASCIITextFile: Boolean = false): Tensor1[A] = stage(ReadBinaryFile(file, isASCIITextFile))
  @rig def writeBinary[A:Num](file: BinaryFile, len: I32)(func: I32 => A): Void = {
    val i = boundVar[I32]
    val f = stageLambda1(i){ func(i) }
    stage(WriteBinaryFile(file, len, f))
  }
  @rig def closeBinary(file: BinaryFile): Void = stage(CloseBinaryFile(file))

  /** Loads the given binary file at `filename` as an Array. */
  @api def loadBinary[T:Num](filename: Text): Tensor1[T] = {
    val file = openBinary(filename, write = false)
    val array = readBinary[T](file)
    closeBinary(file)
    array
  }

  /** Saves the given Array to disk as a binary file at `filename`. */
  @api def writeBinary[T:Num](array: Tensor1[T], filename: Text): Void = {
    val file = openBinary(filename, write = true)
    writeBinary(file, array.length){i => array(i) }
    closeBinary(file)
  }

  /** Creates a placeholder for a numpy array as an @Array. **/
  @api def loadNumpy1D[T:Num](name: String): Tensor1[T] = stage(NumpyArray(name))

  /** Creates a placeholder for a numpy matrix as an @Tensor2.**/
  @api def loadNumpy2D[T:Num](name: String): Tensor2[T] = stage(NumpyMatrix(name))


  /** Loads an ASCII text file. */
  @api def loadASCIITextFile(filename: Text): Tensor1[spatial.dsl.Char] = {
    val file = openBinary(filename, write = false)
    val array = readBinary[spatial.dsl.Char](file, isASCIITextFile = true)
    closeBinary(file)
    array
  }
}
