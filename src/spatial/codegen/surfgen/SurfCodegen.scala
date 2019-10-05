package spatial.codegen.surfgen

import argon._
import argon.codegen.FileDependencies
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import spatial.targets._

// SURF = SLAC Ultimate RTL Framework
trait SurfCodegen extends FileDependencies with AccelTraversal  {
  override val lang: String = "surf"
  override val ext: String = "py"
  override def entryFile: String = s"TopHost.$ext"


  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
    // if (withReturn) emit(src"${b.result}")
  }

  override def copyDependencies(out: String): Unit = {
    val cppResourcesPath = "synth/"

    // // FIXME: Should be OS-independent. Ideally want something that also supports wildcards, maybe recursive copy
    // // Kill old datastructures
    // s"""rm -rf ${out}/datastructures""".!
//
//    dependencies ::= DirDep("synth", "datastructures")
//    dependencies ::= DirDep("synth", "SW")
    dependencies ::= DirDep("synth", "scripts", "../", Some("scripts/"))
    spatialConfig.target match {
      case KCU1500 =>
        dependencies ::= DirDep("synth", "kcu1500.sw-resources", "../")
        dependencies ::= DirDep("synth", "kcu1500.hw-resources", "../")
        dependencies ::= FileDep("synth", "kcu1500.Makefile", "../", Some("Makefile"))
    }

    dependencies ::= FileDep("synth", "build.sbt", "../", Some("build.sbt"))

    dependencies ::= FileDep("synth", "run.sh", "../", Some("run.sh"))

    super.copyDependencies(out)
  }


}
