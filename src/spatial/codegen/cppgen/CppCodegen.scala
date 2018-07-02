package spatial.codegen.cppgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.internal.{spatialConfig => cfg}
import spatial.traversal.AccelTraversal

trait CppCodegen extends FileDependencies with AccelTraversal  {
  override val lang: String = "cpp"
  override val ext: String = "cpp"
  override def entryFile: String = s"TopHost.$ext"


  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
    // if (withReturn) emit(src"${b.result}")
  }

  override def copyDependencies(out: String): Unit = {
    val cppResourcesPath = "synth/cpp-templates"

    // // FIXME: Should be OS-independent. Ideally want something that also supports wildcards, maybe recursive copy
    // // Kill old datastructures
    // s"""rm -rf ${out}/datastructures""".!

    // Register files that are always there for cppgen
    dependencies ::= DirDep(cppResourcesPath, "datastructures")
    dependencies ::= DirDep(cppResourcesPath, "fringeSW")
    dependencies ::= DirDep(cppResourcesPath, "fringeZynq")
    dependencies ::= DirDep(cppResourcesPath, "fringeZCU")
    // dependencies ::= DirDep(cppResourcesPath, "fringeArria10")
    // dependencies ::= DirDep(cppResourcesPath, "fringeDE1SoC")
    dependencies ::= DirDep(cppResourcesPath, "fringeVCS")
    // dependencies ::= DirDep(cppResourcesPath, "fringeXSIM")
    dependencies ::= DirDep(cppResourcesPath, "fringeAWS")


    dependencies ::= DirDep("synth", "scripts", "../", Some("scripts/"))
    dependencies ::= FileDep("synth", "Makefile", "../", Some("Makefile"))
    if (cfg.enableDebugResources) dependencies ::= FileDep("synth", "build.sbt", "../", Some("build.sbt"))
    else dependencies ::= FileDep("synth", "buildDbgResources.sbt", "../", Some("build.sbt"))
    dependencies ::= FileDep("synth", "run.sh", "../", Some("run.sh"))

    // dependencies ::= FileDep(cppResourcesPath, "cpptypes.h")
    // moveDependencies ::= AlwaysDep(s"""${out}/interface.h""", "datastructures")
    // moveDependencies ::= AlwaysDep(s"""${out}/DRAM.h""", "datastructures")
    super.copyDependencies(out)
  }


}
