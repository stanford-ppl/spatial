package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata._
import spatial.metadata.memory._

import scala.collection.mutable

trait PIRSplitGen extends PIRCodegen {

  private var splitting = false
  private var lineCount = 0

  val splitThreshold = 10

  var splitCount = 0

  val scope = mutable.ListBuffer[Lhs]()

  override def emitStm(lhs:Lhs, tp:String, rhsStr:Any):Unit = {
    emit(src"""val $lhs = save("$lhs", $rhsStr) // ${comment(lhs.sym)}""")
    typeMap += lhs -> tp

    scope += lhs
    if (splitting) {
      lineCount += 1
      if (lineCount > splitThreshold) {
        splitEnd
        splitStart
      }
    }
  }

  def splitStart = {
    splitCount += 1
    lineCount = 0
    scope.clear
    emit(s"def split${splitCount} = {")
  }
  def splitEnd = {
    emit(s"}; split${splitCount}")
  }

  override def emitAccelHeader = {
    super.emitAccelHeader
    emit(s"val nameSpace = scala.collection.mutable.Map[String,Any]()")
    emit(s"def lookup[T](name:String) = nameSpace(name).asInstanceOf[T]")
    emit(s"def save[T](name:String, x:T) = { nameSpace(name) = x; x }")
    splitting = true
    splitCount = 0
    splitStart
  }

  override def emitAccelFooter = { 
    splitEnd
    splitting = false
    super.emitAccelFooter
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case x:Lhs if (typeMap.contains(x) && !scope.contains(x)) => 
      s"""lookup[${typeMap(x)}]("${super.quoteOrRemap(x)}")"""
    case x:Sym[_] => quoteOrRemap(Lhs(x))
    case x => super.quoteOrRemap(x)
  }

}
