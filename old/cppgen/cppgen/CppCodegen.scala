package argon.codegen.cppgen

import argon.core._
import argon.codegen.{Codegen, FileDependencies}
import sys.process._
import scala.language.postfixOps
import scala.collection.mutable

trait CppCodegen extends Codegen with FileDependencies  {
  override val name = "Cpp Codegen"
  override val lang: String = "cpp"
  override val ext: String = "cpp"

  var controllerStack = scala.collection.mutable.Stack[Exp[_]]()
  var argOuts: List[Sym[_]] = List()
  var argIOs: List[Sym[_]] = List()
  var argIns: List[Sym[_]] = List()
  var drams: List[Sym[_]] = List()
  var setMems = List[String]()
  var getMems = List[String]()

  override protected def emitBlock(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// results in ${b.result}")
  }

  final protected def emitController(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// ctrl results in ${b.result}")
  }

  override def quote(s: Exp[_]): String = s match {
    case c: Const[_] => quoteConst(c)
    case b: Bound[_] => s"b${b.id}"
    case lhs: Sym[_] => s"x${lhs.id}"
  }

  override def copyDependencies(out: String): Unit = {
    val cppResourcesPath = "cppgen"

    // FIXME: Should be OS-independent. Ideally want something that also supports wildcards, maybe recursive copy
    // Kill old datastructures
    s"""rm -rf ${out}/datastructures""".!
    // Register files that are always there for cppgen
    // TODO: Matt
    dependencies ::= DirDep(cppResourcesPath, "datastructures")
    dependencies ::= DirDep(cppResourcesPath, "fringeSW")
    dependencies ::= DirDep(cppResourcesPath, "fringeZynq")
    dependencies ::= DirDep(cppResourcesPath, "fringeZCU")
    dependencies ::= DirDep(cppResourcesPath, "fringeArria10")
    dependencies ::= DirDep(cppResourcesPath, "fringeVCS")
    dependencies ::= DirDep(cppResourcesPath, "fringeXSIM")
    dependencies ::= DirDep(cppResourcesPath, "fringeAWS")
    dependencies ::= DirDep(cppResourcesPath, "utils")
    // dependencies ::= FileDep(cppResourcesPath, "cpptypes.h")
    // moveDependencies ::= AlwaysDep(s"""${out}/interface.h""", "datastructures")
    // moveDependencies ::= AlwaysDep(s"""${out}/DRAM.h""", "datastructures")
    super.copyDependencies(out)
  }

}
