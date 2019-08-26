package spatial.codegen.cppgen

import argon._
import argon.codegen.FileDependencies
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import spatial.targets._

trait CppCodegen extends FileDependencies with AccelTraversal  {
  override val lang: String = "cpp"
  override val ext: String = "cpp"
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

    // // Register files that are always there for cppgen
    dependencies ::= DirDep("synth", "datastructures")
    dependencies ::= DirDep("synth", "SW")
    // dependencies ::= DirDep(cppResourcesPath, "fringeZynq")
    // dependencies ::= DirDep(cppResourcesPath, "fringeZCU")
    // // dependencies ::= DirDep(cppResourcesPath, "fringeArria10")
    // // dependencies ::= DirDep(cppResourcesPath, "fringeDE1SoC")
    // dependencies ::= DirDep(cppResourcesPath, "fringeVCS")
    // // dependencies ::= DirDep(cppResourcesPath, "fringeXSIM")
    // dependencies ::= DirDep(cppResourcesPath, "fringeAWS")


    dependencies ::= DirDep("synth", "scripts", "../", Some("scripts/"))
    spatialConfig.target match {
      case VCS => 
        dependencies ::= DirDep("synth", "vcs.sw-resources", "../")
        dependencies ::= DirDep("synth", "vcs.hw-resources", "../")
        dependencies ::= FileDep("synth", "vcs.Makefile", "../", Some("Makefile"))
      case Zynq => 
        dependencies ::= DirDep("synth", "zynq.sw-resources", "../")
        dependencies ::= DirDep("synth", "zynq.hw-resources", "../")
        dependencies ::= FileDep("synth", "zynq.Makefile", "../", Some("Makefile"))
      case ZedBoard => 
        dependencies ::= DirDep("synth", "zedboard.sw-resources", "../")
        dependencies ::= DirDep("synth", "zedboard.hw-resources", "../")
        dependencies ::= FileDep("synth", "zedboard.Makefile", "../", Some("Makefile"))
      case ZCU => 
        dependencies ::= DirDep("synth", "zcu.sw-resources", "../")
        dependencies ::= DirDep("synth", "zcu.hw-resources", "../")
        dependencies ::= FileDep("synth", "zcu.Makefile", "../", Some("Makefile"))
      case KCU1500 => 
        dependencies ::= DirDep("synth", "kcu1500.sw-resources", "../")
        dependencies ::= DirDep("synth", "kcu1500.hw-resources", "../")
        dependencies ::= FileDep("synth", "kcu1500.Makefile", "../", Some("Makefile"))
      case CXP => 
        dependencies ::= DirDep("synth", "cxp.sw-resources", "../")
        dependencies ::= DirDep("synth", "cxp.hw-resources", "../")
        dependencies ::= FileDep("synth", "cxp.Makefile", "../", Some("Makefile"))
      case AWS_F1 => 
        dependencies ::= DirDep("synth", "aws.sw-resources", "../")
        dependencies ::= DirDep("synth", "aws.hw-resources", "../")
        dependencies ::= FileDep("synth", "aws.Makefile", "../", Some("Makefile"))
      case DE1 => 
        dependencies ::= DirDep("synth", "de1.sw-resources", "../")
        dependencies ::= DirDep("synth", "de1.hw-resources", "../")
        dependencies ::= FileDep("synth", "de1.Makefile", "../", Some("Makefile"))
      case Arria10 => 
        dependencies ::= DirDep("synth", "arria10.sw-resources", "../")
        dependencies ::= DirDep("synth", "arria10.hw-resources", "../")
        dependencies ::= FileDep("synth", "arria10.Makefile", "../", Some("Makefile"))
      case ASIC => 
        dependencies ::= DirDep("synth", "asic.sw-resources", "../")
        dependencies ::= DirDep("synth", "asic.hw-resources", "../")
        dependencies ::= FileDep("synth", "asic.Makefile", "../", Some("Makefile"))
    }
    
    dependencies ::= FileDep("synth", "build.sbt", "../", Some("build.sbt"))

    dependencies ::= FileDep("synth", "run.sh", "../", Some("run.sh"))

    super.copyDependencies(out)
  }


}
