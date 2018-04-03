package spatial.codegen.chiselgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.internal.{spatialConfig => cfg}

trait ChiselCodegen extends FileDependencies  {
  override val lang: String = "chisel"
  override val ext: String = "scala"
  override def entryFile: String = s"RootController_1.$ext"

  var streamLines = collection.mutable.Map[String, Int]() // Map from filename number of lines it has
  var streamExtensions = collection.mutable.Map[String, Int]() // Map from filename to number of extensions it has
  var topLayerTraits = List[String]() // List of top layer nodes, used in enableSplitting generation
  val tabWidth: Int = 2
  val maxLinesPerFile = 200
  var compressorMap = collection.mutable.HashMap[String, (String,Int)]()


  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    inGenn(out, "RootController", ext) {
      exitAccel()
      visitBlock(b)
      enterAccel()
    }
    // if (withReturn) emitt(src"${b.result}")
  }

  final protected def inGennAll[T](out: String, base: String, ext: String)(blk: => T): Unit = {
    for (i <- 1 until streamExtensions(base)+1) {
      inGen(out, base + "_" + i + "." + ext)(blk)
    }
  }

  final protected def inGenn[T](out: String, base: String, ext: String)(blk: => T): Unit = {
    // Lookup current split extension and number of lines
    if (!streamExtensions.contains(base)) streamExtensions += base -> 1
    val currentOverflow = s"_${streamExtensions(base)}"
    if (!streamLines.contains(base + currentOverflow + "." + ext)) streamLines += {base + currentOverflow + "." + ext} -> 0
    inGen(out, base + currentOverflow + "." + ext)(blk)
  }

  protected def emitt(x: String): Unit = {
    val lineCount = streamLines(state.streamName.split("/").last)
    streamLines(state.streamName.split("/").last) += 1
    if (lineCount > maxLinesPerFile) Console.println("exceeded!")
    emit(x)
  }

  final protected def emitGlobalWire(x: String, forceful: Boolean = false): Unit = {
    inGenn(out, "GlobalWires", ext) {
      val on = config.enGen
      if (forceful) {config.enGen = true}
      emitt(x)
      config.enGen = on
    }
  }

  final protected def emitGlobalWireMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val module_type = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    if (cfg.compressWires == 1 | cfg.compressWires == 2) {
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == module_type).size
        compressorMap += (lhs -> (module_type, id))
      }
    } else {
      if (compressorMap.contains(lhs)) {
        emitGlobalWire(src"// val $lhs = $rhs already emitted", forceful)
      } else {
        compressorMap += (lhs -> (module_type, 0))
        emitGlobalWire(src"val $lhs = $rhs", forceful)
      }
    }
  }

  // final protected def emitGlobalRetimeMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
  //   val module_type = rhs.replace(" ", "")
  //   if (config.multifile == 5 | config.multifile == 6) {
  //     // Assume _retime values only emitted once
  //     val id = compressorMap.values.map(_._1).filter(_ == "_retime").size
  //     compressorMap += (lhs -> ("_retime", id))
  //     retimeList += rhs
  //   } else {
  //     emitGlobalWire(src"val $lhs = $rhs", forceful)
  //   }
  // }

  // final protected def emitGlobalModuleMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
  //   val module_type_white = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
  //   var rtid = "na"
  //   if (config.multifile == 5 | config.multifile == 6) {
  //     val module_type = if (module_type_white.contains("retime=")) {
  //       val extract = ".*retime=rt\\(([0-9]+)\\),.*".r
  //       val extract(x) = module_type_white
  //       rtid = x
  //       module_type_white.replace(s"retime=rt(${rtid}),","")
  //     } else {
  //       module_type_white
  //     }
  //     if (!compressorMap.contains(lhs)) {
  //       val id = compressorMap.values.map(_._1).filter(_ == module_type).size
  //       compressorMap += (lhs -> (module_type, id))
  //       if (rtid != "na") {
  //         pipeRtMap += ((module_type, id) -> rtid)
  //       }
  //     }
  //   } else {
  //     val module_type = module_type_white
  //     if (compressorMap.contains(lhs)) {
  //       emitGlobalModule(src"// val $lhs = $rhs already emitted", forceful)
  //     } else {
  //       compressorMap += (lhs -> (module_type, 0))
  //       emitGlobalModule(src"val $lhs = $rhs", forceful)
  //     }
  //   }
  // }

  // final protected def emitInstrumentation(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("Instrumentation")) {
  //     emitt(x, forceful)
  //   }
  // }

  // final protected def emitGlobalModule(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalModules")) {
  //     emitt(x, forceful)
  //   }
  // }

  // final protected def emitGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalRetiming")) {
  //     emitt(x, forceful)
  //   }
  // }

  // final protected def openGlobalWire(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalWires")) {
  //     open(x, forceful)
  //   }
  // }

  // final protected def openInstrumentation(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("Instrumentation")) {
  //     open(x, forceful)
  //   }
  // }

  // final protected def openGlobalModule(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalModules")) {
  //     open(x, forceful)
  //   }
  // }

  // final protected def openGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalRetiming")) {
  //     open(x, forceful)
  //   }
  // }

  // final protected def closeGlobalWire(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalWires")) {
  //     close(x, forceful)
  //   }
  // }

  // final protected def closeInstrumentation(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("Instrumentation")) {
  //     close(x, forceful)
  //   }
  // }

  // final protected def closeGlobalModule(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalModules")) {
  //     close(x, forceful)
  //   }
  // }

  // final protected def closeGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
  //   withStream(getStream("GlobalRetiming")) {
  //     close(x, forceful)
  //   }
  // }


  override def copyDependencies(out: String): Unit = {
    val resourcesPath = "synth/chisel-templates"

    dependencies ::= DirDep(resourcesPath, "templates", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "hardfloat", relPath = "template-level/templates")
    dependencies ::= DirDep(resourcesPath, "fringeHW", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeZynq", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeASIC", relPath = "template-level/")
    // dependencies ::= DirDep(resourcesPath, "fringeDE1SoC", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeVCS", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeXSIM", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeAWS", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeArria10", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "fringeASIC", relPath = "template-level/")
    dependencies ::= DirDep(resourcesPath, "scripts", "../", Some("scripts/"))

    dependencies ::= FileDep(resourcesPath, "Top.scala", outputPath = Some("Top.scala"))

    super.copyDependencies(out)
  }


}
