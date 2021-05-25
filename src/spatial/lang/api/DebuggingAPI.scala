package spatial.lang.api

import argon._
import argon.node.TextConcat

import forge.tags._
import utils.implicits.collections._

trait DebuggingAPI_Internal extends argon.lang.api.DebuggingAPI_Internal 

trait DebuggingAPI_Shadowing extends DebuggingAPI_Internal
  with argon.lang.api.DebuggingAPI_Shadowing {
  this: StaticAPI_Shadowing =>

  @virtualize
  @api def sleep(cycles: I32): Void = Pipe.NoBind.Foreach(cycles by 1){i => if (i == 0) print(i) }

  /** Prints the given Array to the console, preceded by an optional heading. **/
  @virtualize
  @api def printArray[T:Type](array: Tensor1[T], header: Text = Text("")): Void = {
    println(header)
    (0 until array.length).foreach{i => print(array(i).toString + " ") }
    println("")
  }

  /** Prints the given Matrix to the console, preceded by an optional heading. **/
  @virtualize
  @api def printMatrix[T:Type](matrix: Tensor2[T], header: Text = Text("")): Void = {
    println(header)
    (0 until matrix.rows) foreach { i =>
      (0 until matrix.cols) foreach { j =>
        print(matrix(i, j).toString + "\t")
      }
      println("")
    }
  }

  /** Prints the given Tensor3 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printTensor3[T:Type](tensor: Tensor3[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          print(tensor(i, j, k).toString + "\t")
        }
        println("")
      }
      (0 until tensor.dim2) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given Tensor4 to the console, preceded by the an optional heading. **/
  @virtualize
  @api def printTensor4[T:Type](tensor: Tensor4[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          (0 until tensor.dim3) foreach { l =>
            print(tensor(i, j, k, l).toString + "\t")
          }
          println("")
        }
        (0 until tensor.dim3) foreach {_ => print("--\t")}
        println("")
      }
      (0 until tensor.dim3) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given Tensor5 to the console, preceded by the an optional heading. **/
  @virtualize
  @api def printTensor5[T:Type](tensor: Tensor5[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          (0 until tensor.dim3) foreach { l =>
            (0 until tensor.dim4) foreach { m =>
              print(tensor(i, j, k, l, m).toString + "\t")
            }
            println("")
          }
          (0 until tensor.dim4) foreach {_ => print("--\t")}
          println("")
        }
        (0 until tensor.dim4) foreach {_ => print("--\t")}
        println("")
      }
      (0 until tensor.dim4) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given SRAM1 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM1[T:Type](array: SRAM1[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until array.length) { i => print(array(i).toString + " ") }
    println("")
  }

  /** Prints the given SRAM2 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM2[T:Type](matrix: SRAM2[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until matrix.rows){ i =>
      Foreach(0 until matrix.cols){ j =>
        print(matrix(i, j).toString + "\t")
      }
      println("")
    }
  }

  /** Prints the given SRAM3 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM3[T:Type](tensor: SRAM3[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until tensor.dim0) { i =>
      Foreach(0 until tensor.dim1) { j =>
        Foreach(0 until tensor.dim2) { k =>
          print(tensor(i, j, k).toString + "\t")
        }
        println("")
      }
      Foreach(0 until tensor.dim2) {_ => print("--\t")}
      println("")
    }
  }

  implicit class I32Range(x: Series[I32]) {
    @api def foreach(func: I32 => Void): Void = Foreach(x){i => func(i) }
  }


  implicit class Quoting(sc: StringContext) {
    @api def r(args: Any*): Text = {
      val quotedArgs = args.toArray.map{
        case t: Top[_] => t.toText
        case t         => Text(t.toString)
      }
      val quotedParts = sc.parts.map(Text.apply)

      val str = quotedParts.interleave(quotedArgs)
      stage(TextConcat(str))
    }
  }

  private val defaultMargin = 0.1
  @api def approxEql[T:Bits](a:T, b:T, margin:scala.Double=0.1):Bit = {
    implicitly[Type[T]] match {
      case tobits:Num[_] =>
        implicit val n = tobits.asInstanceOf[Num[T]]
        abs(a - b) <= (margin.to[T] * abs(a))
      case _ => a === b
    }
  }

  @api def approxEql[T:Bits](a:Tensor1[T], b:Tensor1[T]):Bit = approxEql(a,b,0.1)
  @api def approxEql[T:Bits](a:Tensor1[T], b:Tensor1[T], margin:scala.Double):Bit = {
    (a.length === b.length) &
    a.zip(b) { (a,b) => approxEql[T](a,b,margin) }.reduce { _ & _ }
  }

  @api def approxEql[T:Bits](a:Tensor2[T], b:Tensor2[T]):Bit = approxEql(a,b,0.1)
  @api def approxEql[T:Bits](a:Tensor2[T], b:Tensor2[T], margin:scala.Double):Bit = {
    a.length === b.length &
    a.zip(b) { (a,b) => approxEql[T](a,b,margin) }.reduce { _ & _ }
  }

  @api def approxEql[T:Bits](a:Tensor3[T], b:Tensor3[T]):Bit = approxEql(a,b,0.1)
  @api def approxEql[T:Bits](a:Tensor3[T], b:Tensor3[T], margin:scala.Double):Bit = {
    (a.length === b.length) &
    a.zip(b) { (a,b) => approxEql(a,b,margin) }.reduce { _ & _ }
  }


  @api def checkGold[T:Bits](dram:DRAM1[T], gold:Array[T])(implicit ev:Cast[Text,T]):Bit =
    checkGold(dram, gold, 0)
  @api def checkGold[T:Bits](dram:DRAM1[T], gold:Array[T], margin:scala.Double)(implicit ev:Cast[Text,T]):Bit = {
    val result = getMem(dram)
    println(s"${dram.name.getOrElse(s"$dram")} Result: ")
    printArray(result)

    println(s"${dram.name.getOrElse(s"$dram")} Gold: ")
    printArray(gold)

    approxEql[T](result, gold, margin)
  }

  @api def checkGold[T:Bits](dram:DRAM2[T], gold:Tensor2[T])(implicit ev:Cast[Text,T]):Bit =
    checkGold(dram, gold, 0)
  @api def checkGold[T:Bits](dram:DRAM2[T], gold:Tensor2[T], margin:scala.Double)(implicit ev:Cast[Text,T]):Bit = {
    val result = getMatrix(dram)
    println(s"${dram.name.getOrElse(s"$dram")} Result: ")
    printMatrix(result)

    println(s"${dram.name.getOrElse(s"$dram")} Gold: ")
    printMatrix(gold)

    approxEql[T](result, gold, margin)
  }

  @api def checkGold[T:Bits](reg:Reg[T], gold:T)(implicit ev:Cast[T,Text]):Bit = checkGold(reg, gold, 0)
  @api def checkGold[T:Bits](reg:Reg[T], gold:T,margin:scala.Double)(implicit ev:Cast[T,Text]):Bit = {
    val result = getArg(reg)
    println(Text(s"${reg.name.getOrElse(s"$reg")} Result: ") + result.to[Text])
    println(Text(s"${reg.name.getOrElse(s"$reg")} Gold: ") + gold.to[Text])
    approxEql[T](result, gold, margin)
  }

  // Unstaged if statement
  def If(cond:scala.Boolean)(block: => Unit) = {
    cond match {
      case true => block
      case _ =>
    }
  }

  // Unstaged if else statement
  def IfElse[T](cond:scala.Boolean)(trueBlock: => T)(falseBlock: => T) = {
    cond match {
      case true => trueBlock
      case false => falseBlock
    }
  }

}
