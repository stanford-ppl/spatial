package spatial.codegen.chiselgen

import argon.NoBitWidthException
import argon.codegen.{Codegen, FileDependencies}
import argon.core._
import argon.nodes._
import spatial.nodes._
import spatial.aliases._

import java.io.PrintWriter
import scala.collection.mutable
import scala.io.Source
import scala.util.matching.Regex

trait ChiselCodegen extends Codegen with FileDependencies { // FileDependencies extends Codegen already
  override val name = "Chisel Codegen"
  override val lang: String = "chisel"
  override val ext: String = "scala"
  var controllerStack = scala.collection.mutable.Stack[Exp[_]]()

  var alphaconv = mutable.HashMap[String, String]() // Map for tracking defs of nodes and if they get redeffed anywhere, we map it to a suffix
  var maxretime: Int = 0 // Look for the biggest retime in the app and have the app wait this many cycles before enabling root controllerStack
  var disableSplit: Boolean = false // Temporary hack to avoid splitting files from overflow while emitting code that belongs together

  final def alphaconv_register(xx: String): Unit = {
    val x = "_reuse[0-9]+".r.replaceAllIn(xx, "")
    if (alphaconv.contains(x)) {
      val suf = alphaconv(x).replace("_reuse","")
      if (suf == "") {
        alphaconv += (x -> "_reuse1") // If already used, increment suffix
      } else {
        val newsuf = suf.toInt + 1
        alphaconv += (x -> s"_reuse$newsuf")
      }
    } else {
      alphaconv += (x -> "") // Otherwise don't suffix it
    }
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case e: Exp[_] => wireMap(quote(e) + alphaconv.getOrElse(quote(e), ""))
    case _ => super.quoteOrRemap(arg)
  }

  override protected def emitBlock(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// results in ${b.result}")
  }

  /**
   * Add global imports after-the-fact to all streams after codegen finishes. This is necessary
   * to guarantee all imports are known (due to file-splitting). GlobalWires and GlobalModules are
   * instantiated as individual objects owned by RootController, and all other traits import the
   * contents of those objects into scope. It is done this way instead of as singletons in order to
   * keep everything inside the AccelTop module.
   */
  override protected def postprocess[S:Type](block: Block[S]): Block[S] = {
    emitGlobalInstancesAndImports("RootController")
    fixupGlobalDeps()

    val streams = streamMapReverse.keySet.toSet.map {
      f:String => f.split('.').dropRight(1).mkString(".")  /*strip extension */
    }.filterNot(s => s == "RootController" ||
      s.startsWith("AccelTop") ||
      s.startsWith("GlobalModules") ||
      s.startsWith("GlobalWires") ||
      s.startsWith("IOModule") ||
      s.startsWith("Instantiator") ||
      s.startsWith("Instrumentation") ||
      s.contains("Mixer")
    )
    for (s <- streams) {
      emitGlobalImports(s)
    }

    super.postprocess(block)
  }

  final protected def emitController(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// results in ${b.result}")
  }

  final protected def emitGlobalWire(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalWires")) {
      emit(x, forceful)
    }
  }

  final protected def emitGlobalWireMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val stripped = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    if (config.multifile == 5 | config.multifile == 6) {
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == stripped).size
        compressorMap += (lhs -> (stripped, id))
      }
    } else {
      if (compressorMap.contains(lhs)) {
        emitGlobalWire(src"// val $lhs = $rhs already emitted", forceful)
      } else {
        compressorMap += (lhs -> (stripped, 0))
        emitGlobalWire(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitGlobalRetimeMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val stripped = rhs.replace(" ", "")
    if (config.multifile == 5 | config.multifile == 6) {
      // Assume _retime values only emitted once
      val id = compressorMap.values.map(_._1).filter(_ == "_retime").size
      compressorMap += (lhs -> ("_retime", id))
      retimeList += rhs
    } else {
      emitGlobalWire(src"val $lhs = $rhs", forceful)
    }
  }

  final protected def emitGlobalModuleMap(lhs: String, rhs: String, forceful: Boolean = false): Unit = {
    val stripped_white = rhs.replace("new ", "newnbsp").replace(" ", "").replace("nbsp", " ")
    var rtid = "na"
    if (config.multifile == 5 | config.multifile == 6) {
      val stripped = if (stripped_white.contains("retime=")) {
        val extract = ".*retime=rt\\(([0-9]+)\\),.*".r
        val extract(x) = stripped_white
        rtid = x
        stripped_white.replace(s"retime=rt(${rtid}),","")
      } else {
        stripped_white
      }
      if (!compressorMap.contains(lhs)) {
        val id = compressorMap.values.map(_._1).filter(_ == stripped).size
        compressorMap += (lhs -> (stripped, id))
        if (rtid != "na") {
          pipeRtMap += ((stripped, id) -> rtid)
        }
      }
    } else {
      val stripped = stripped_white
      if (compressorMap.contains(lhs)) {
        emitGlobalModule(src"// val $lhs = $rhs already emitted", forceful)
      } else {
        compressorMap += (lhs -> (stripped, 0))
        emitGlobalModule(src"val $lhs = $rhs", forceful)
      }
    }
  }

  final protected def emitInstrumentation(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("Instrumentation")) {
      emit(x, forceful)
    }
  }

  final protected def emitGlobalModule(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalModules")) {
      emit(x, forceful)
    }
  }

  final protected def emitGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalRetiming")) {
      emit(x, forceful)
    }
  }

  final protected def openGlobalWire(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalWires")) {
      open(x, forceful)
    }
  }

  final protected def openInstrumentation(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("Instrumentation")) {
      open(x, forceful)
    }
  }

  final protected def openGlobalModule(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalModules")) {
      open(x, forceful)
    }
  }

  final protected def openGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalRetiming")) {
      open(x, forceful)
    }
  }

  final protected def closeGlobalWire(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalWires")) {
      close(x, forceful)
    }
  }

  final protected def closeInstrumentation(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("Instrumentation")) {
      close(x, forceful)
    }
  }

  final protected def closeGlobalModule(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalModules")) {
      close(x, forceful)
    }
  }

  final protected def closeGlobalRetiming(x: String, forceful: Boolean = false): Unit = {
    withStream(getStream("GlobalRetiming")) {
      close(x, forceful)
    }
  }

  protected def globalModules(): List[String] = {
    streamExtensions.getOrElse("GlobalModules", Nil).map { i =>
      if (i == 0) "GlobalModules" else src"GlobalModules_${i}"
    }
  }

  protected def globalWires(): List[String] = {
    streamExtensions.getOrElse("GlobalWires", Nil).map { i =>
      if (i == 0) "GlobalWires" else src"GlobalWires_${i}"
    }
  }

  protected def instrumentation(): List[String] = {
    streamExtensions.getOrElse("Instrumentation", Nil).map { i =>
      if (i == 0) "Instrumentation" else src"Instrumentation_${i}"
    }
  }

  /**
   * Helper used to insert a set of lines at a given string into a stream after-the-fact, then
   * resets the stream to the end of the file. This is necessary if the lines we want to include
   * are not known until the end of generation (due to file splitting). If a `replace` function
   * is supplied, it will be tried on every line of the input.
   */
  protected def emitLinesAt(name: String, afterLinePrefix: String, lines: List[String],
                            replace: Option[(Regex, String)] = None): Unit = {
    def tryReplace(l: String) = replace match {
      case Some((regex, replacement)) => regex.replaceAllIn(l, replacement)
      case None => l
    }
    val sName = s"$name.$ext"
    val fName = s"$out$name.$ext"
    val oldStream = streamMapReverse(sName)
    oldStream.close()

    val oldContents = Source.fromFile(fName).getLines.toList
    val insertAt = oldContents.indexWhere(_.startsWith(afterLinePrefix))+1
    val newStream = new PrintWriter(fName)
    for (l <- oldContents.take(insertAt)) {
      newStream.println(tryReplace(l))
    }
    newStream.println(lines.mkString("\n"))
    for (l <- oldContents.drop(insertAt)) {
      newStream.println(tryReplace(l))
    }
    newStream.flush()
    newStream.close()

    streamMapReverse(sName) = newStream
  }

  /**
   * Adds instance declarations and imports to a stream after-the-fact, then resets the stream to
   * the end of the file. This generates all of the values that are then used by @link{emitGlobalImports}.
   * It should only be called for one stream (the owner of all the global instances).
   */
  protected def emitGlobalInstancesAndImports(name: String): Unit = {
    val decls = (globalWires ++ globalModules ++ instrumentation).map(s => src"  val inst_${s} = new ${s}(this)")
    val imports = (globalWires ++ globalModules).map(s => src"  import inst_${s}._")
    emitLinesAt(name, "trait", decls ++ imports)
  }

  /**
   * Imports all values emitted by @link{emitGlobalInstancesAndImports} to a stream. The stream
   * must extend from the trait that was the target of @link{emitGlobalInstancesAndImports}.
   */
  protected def emitGlobalImports(name: String): Unit = {
    val imports = globalWires ++ globalModules
    emitLinesAt(name, "trait", imports.map(s => src"  import inst_${s}._"))
  }

  /**
   * Import all instances of GlobalModules / GlobalWires (except self) into the current scope,
   * so that GlobalWires can access other wires and GlobalModules can access all wires and other
   * modules. All imports are via the RootController, which owns all the instances.
   */
  protected def fixupGlobalDeps(): Unit = {
    val allGlobalModules = globalModules
    val allGlobalWires = globalWires
    val allInstrumentation = instrumentation
    for (gm <- allGlobalModules) {
      val imports = (allGlobalModules ++ allGlobalWires).filterNot(_ == gm)
      emitLinesAt(gm, "class", imports.map(s => src"  import rc.inst_${s}._"))
    }
    for (gw <- allGlobalWires) {
      val imports = allGlobalWires.filterNot(_ == gw)
      emitLinesAt(gw, "class", imports.map(s => src"  import rc.inst_${s}._"))
    }
    for (i <- allInstrumentation) {
      val imports = allGlobalWires
      emitLinesAt(i, "class", imports.map(s => src"  import rc.inst_${s}._"))
    }
  }

  protected def bitWidth(tp: Type[_]): Int = {
    c"$tp" match {
      case "Avalon" => 32
      case _ => throw new NoBitWidthException(tp)
    }
  }

  override def copyDependencies(out: String): Unit = {
    // s"mkdir ${out}${java.io.File.separator}templates" !
    // s"mkdir ${out}${java.io.File.separator}templates".!
    // dependencies.foreach{dep => if (dep.needsCopy) {
    //   log(s"Copying ${dep.input} to $out")
    //   s"cp ${dep.input} ${out}${java.io.File.separator}templates${java.io.File.separator}${dep.outputPath}" !
    // }}
    val resourcesPath = s"chiselgen"

    dependencies ::= DirDep(resourcesPath, "template-level/emul")
    dependencies ::= DirDep(resourcesPath, "template-level/templates")
    // dependencies ::= DirDep(resourcesPath, "template-level/templates/hardfloat")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeHW")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeZynq")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeASIC")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeDE1SoC")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeVCS")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeXSIM")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeAWS")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeArria10")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeASIC")
    dependencies ::= DirDep(resourcesPath, "template-level/fringeVCU1525")
    dependencies ::= DirDep(resourcesPath, "app-level/scripts", "../", Some("scripts/"))

    dependencies ::= FileDep(resourcesPath, "app-level/Makefile", "../", Some("Makefile"))
    dependencies ::= FileDep(resourcesPath, "app-level/build.sbt", "../", Some("build.sbt"))
    dependencies ::= FileDep(resourcesPath, "app-level/run.sh", "../", Some("run.sh"))
    dependencies ::= FileDep(resourcesPath, "app-level/Top.scala", outputPath = Some("Top.scala"))

    super.copyDependencies(out)
  }


  def tabbing(stream: String): String = " "*(tabWidth*(streamTab getOrElse (stream, 0)))

  protected def strip_ext(name: String): String = {"\\..*".r.replaceAllIn(name,"")}
  protected def get_ext(name: String): String = {".*\\.".r.replaceAllIn(name,"")}
  protected def get_real_stream(curStream: String): String = {
    if ((curStream contains "IOModule") ||
        (curStream contains "AccelTop") ||
        (curStream contains "Instantiator")) {
      strip_ext(curStream)
    } else {
      val current_ext = streamExtensions(strip_ext(curStream)).last
      val cur_stream_ext = if (current_ext == 0) {strip_ext(curStream)} else {strip_ext(curStream) + "_" + current_ext}
      val cur_tabbing = streamTab(cur_stream_ext + "." + get_ext(curStream))
      streamLines(strip_ext(curStream)) += 1
      val global_lines = streamLines(strip_ext(curStream))
      val file_num = global_lines / maxLinesPerFile
      if ((cur_tabbing == 1) && !disableSplit && (!streamExtensions(strip_ext(curStream)).contains(file_num))) {
        val next = newStream(strip_ext(curStream) + "_" + file_num)
        val curlist = streamExtensions(strip_ext(curStream))

        // Console.println(s"   Just appended ${file_num} to list for ${strip_ext(curStream)}!")
        streamExtensions += (strip_ext(curStream) -> {curlist :+ file_num})

        val defn = if ((curStream contains "GlobalWires") ||
          (curStream contains "GlobalModules") ||
          (curStream contains "Instrumentation")
        ) {
          src"""
@chiselName
class ${strip_ext(curStream)}_${file_num}(rc: RootController) {
  import rc._
"""
        } else {
          val prnt = if (curlist.last == 0) src"${strip_ext(curStream)}" else src"${strip_ext(curStream)}_${curlist.last}"
          src"""trait ${strip_ext(curStream)}_${file_num} extends ${prnt} {"""
        }
        withStream(next) {
          stream.println(src"""package accel
import templates._
import templates.ops._
import types._
import chisel3._
import chisel3.util._
import chisel3.experimental.chiselName

$defn
""")
        val methodized_trait_pattern = "^x[0-9]+".r
        val new_trait_name = src"""${strip_ext(curStream)}_${file_num}"""
        if (config.multifile == 6 & methodized_trait_pattern.findFirstIn(new_trait_name).isDefined) {
          stream.println(src"""def method_${strip_ext(curStream)}_${file_num}() {""")
        }

          streamTab(strip_ext(curStream) + "_" + file_num + "." + get_ext(curStream)) = 1

        }
      }
      cur_stream_ext
    }

  }



  override protected def emit(x: String, forceful: Boolean = false): Unit = {
    if (emitEn | forceful) {

      // val current_ext = streamExtensions(strip_ext(streamName)).last
      // val cur_stream_ext = if (current_ext == 0) {strip_ext(streamName)} else {strip_ext(streamName) + "_" + current_ext}
      // val cur_tabbing = streamTab(cur_stream_ext + "." + get_ext(streamName))
      // val global_lines = streamLines(strip_ext(streamName))
      // val ext_list = streamExtensions(strip_ext(streamName))
      // val file_num = global_lines / maxLinesPerFile
      // val debug_stuff = "// lines " + global_lines + " tabbing " + cur_tabbing + " ext_list " + ext_list + " file_num " + file_num

      val realstream = get_real_stream(streamName)
      withStream(getStream(realstream)) {stream.println(tabbing(realstream + "." + get_ext(streamName)) + x /*+ debug_stuff*/)}
    } else {
      if (config.emitDevel == 2) {Console.println(s"[ ${lang}gen-NOTE ] Emission of ${x} does not belong in this backend")}
    }
  }
  override protected def open(x: String, forceful: Boolean = false): Unit = {
    if (emitEn | forceful) {
      val realstream = get_real_stream(streamName)
      withStream(getStream(realstream)) {stream.println(tabbing(realstream + "." + get_ext(streamName)) + x)};
      if (streamTab contains {realstream + "." + get_ext(streamName)}) streamTab(realstream + "." + get_ext(streamName)) += 1
    } else {
      if (config.emitDevel == 2) {Console.println(s"[ ${lang}gen-NOTE ] Emission of ${x} does not belong in this backend")}
    }
  }
  override protected def close(x: String, forceful: Boolean = false): Unit = {
    if (emitEn | forceful) {
      val realstream = get_real_stream(streamName)
      if (streamTab contains {realstream + "." + get_ext(streamName)}) {
        streamTab(realstream + "." + get_ext(streamName)) -= 1;
        withStream(getStream(realstream)) {stream.println(tabbing(realstream + "." + get_ext(streamName)) + x)}
      }
    } else {
      if (config.emitDevel == 2) {Console.println(s"[ ${lang}gen-NOTE ] Emission of ${x} does not belong in this backend")}
    }
  }
  override protected def closeopen(x: String, forceful: Boolean = false): Unit = { // Good for "} else {" lines
    if (emitEn | forceful) {
      val realstream = get_real_stream(streamName)
      if (streamTab contains {realstream + "." + get_ext(streamName)}) {
        streamTab(realstream + "." + get_ext(streamName)) -= 1;
        withStream(getStream(realstream)) {stream.println(x);}
        streamTab(realstream + "." + get_ext(streamName)) += 1
      }
    } else {
      if (config.emitDevel == 2) {Console.println(s"[ ${lang}gen-NOTE ] Emission of ${x} does not belong in this backend")}
    }
  }

  override protected def openMethodWrapper(sym: String, prefix: String = "m", aggressiveness: Int = 1): Unit = {
    val saveDisableSplit = disableSplit
    disableSplit = true
    super.openMethodWrapper(sym, prefix, aggressiveness)
    disableSplit = saveDisableSplit
  }

  override protected def closeMethodWrapper(sym: String, prefix: String = "m", aggressiveness: Int = 1, saveResult: Boolean = false): Unit = {
    val saveDisableSplit = disableSplit
    disableSplit = true
    super.closeMethodWrapper(sym, prefix, aggressiveness, saveResult)
    disableSplit = saveDisableSplit
  }

  final protected def withSubStream[A](name: String, parent: String, inner: Boolean = false)(body: => A): A = { // Places body inside its own trait file and includes it at the end
    if (config.multifile == 6) {
      // Console.println(s"substream $name, parent $parent ext ${streamExtensions(parent)}")
      val prnts = if (!(streamExtensions contains parent)) src"$parent" else streamExtensions(parent).map{i => if (i == 0) src"$parent" else src"${parent}_${i}"}.mkString(" with ")
      emit(src"// Creating sub kernel ${name}")
      withStream(newStream(name)) {
          emit(src"""package accel
import templates._
import templates.ops._
import types._
import chisel3._
import chisel3.util._

""")
          open(src"""trait ${name} extends ${prnts} {
def method_${name}() {""")
          try { body }
          finally {
            streamExtensions(name).foreach{i =>
              val fname = if (i == 0) src"$name" else src"${name}_${i}"
              withStream(getStream(fname)) { stream.println("}}")}
            }
          }
      }
    } else if (config.multifile == 5) {
      // Console.println(s"substream $name, parent $parent ext ${streamExtensions(parent)}")
      val prnts = if (!(streamExtensions contains parent)) src"$parent" else streamExtensions(parent).map{i => if (i == 0) src"$parent" else src"${parent}_${i}"}.mkString(" with ")
      emit(src"// Creating sub kernel ${name}")
      withStream(newStream(name)) {
          emit(src"""package accel
import templates._
import templates.ops._
import types._
import chisel3._
import chisel3.util._

""")
          open(src"""trait ${name} extends ${prnts} {""")
          try { body }
          finally {
            streamExtensions(name).foreach{i =>
              val fname = if (i == 0) src"$name" else src"${name}_${i}"
              withStream(getStream(fname)) { stream.println("}")}
            }
          }
      }
    } else if (config.multifile == 4) {
      // Console.println(s"substream $name, parent $parent ext ${streamExtensions(parent)}")
      val prnts = if (!(streamExtensions contains parent)) src"$parent" else streamExtensions(parent).map{i => if (i == 0) src"$parent" else src"${parent}_${i}"}.mkString(" with ")
      emit(src"// Creating sub kernel ${name}")
      withStream(newStream(name)) {
          emit(src"""package accel
import templates._
import templates.ops._
import types._
import chisel3._
import chisel3.util._

""")
          open(src"""trait ${name} extends ${prnts} {""")
          try { body }
          finally {
            streamExtensions(name).foreach{i =>
              val fname = if (i == 0) src"$name" else src"${name}_${i}"
              withStream(getStream(fname)) { stream.println("}") }
              getStream(fname).close()
            }
          }
      }
    } else if (config.multifile == 3 & inner) {
        withStream(newStream(name)) {
            emit(src"""package accel
  import templates._
  import templates.ops._
  import types._
  import chisel3._
  import chisel3.util._

  """)
            open(src"""trait ${name} extends RootController {""")
            try { body }
            finally {
              close("}")
            }
        }

    } else if (config.multifile == 2) {
      open(src";{ // Multifile disabled, emitting $name kernel here")
      try { body }
      finally { close("}") }
    } else if (config.multifile == 1 & inner) {
      open(src";{ // Multifile disabled, emitting $name kernel here")
      try { body }
      finally { close("}") }
    } else {
      open(src"// Multifile disabled, emitting $name kernel here without scoping")
      try { body }
      finally { close("") }
    }
  }

  protected def newWire(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => src"new FixedPoint($s, $d, $f)"
    case IntType() => "UInt(32.W)"
    case LongType() => "UInt(32.W)"
    case FltPtType(m,e) => src"new FloatingPoint($m, $e)"
    case BooleanType => "Bool()"
    case tp: VectorType[_] => src"Vec(${tp.width}, ${newWire(tp.typeArguments.head)})"
    case tp: StructType[_] => src"UInt(${bitWidth(tp)}.W)"
    // case tp: IssuedCmd => src"UInt(${bitWidth(tp)}.W)"
    case tp: ArrayType[_] => src"Wire(Vec(999, ${newWire(tp.typeArguments.head)}"
    case _ => throw new argon.NoWireConstructorException(s"$tp")
  }

  def swap(lhs: Exp[_], s: RemapSignal): String = {
    s match {
      case En => wireMap(src"${lhs}_en")
      case Done => wireMap(src"${lhs}_done")
      case Last => wireMap(src"${lhs}_last")
      case BaseEn => wireMap(src"${lhs}_base_en")
      case Mask => wireMap(src"${lhs}_mask")
      case Resetter => wireMap(src"${lhs}_resetter")
      case DatapathEn => wireMap(src"${lhs}_datapath_en")
      case CtrTrivial => wireMap(src"${lhs}_ctr_trivial")
      case IIDone => wireMap(src"${lhs}_II_done")
      case RstEn => wireMap(src"${lhs}_rst_en")
      case CtrEn => wireMap(src"${lhs}_ctr_en")
      case Ready => wireMap(src"${lhs}_ready")
      case Valid => wireMap(src"${lhs}_valid")
      case NowValid => wireMap(src"${lhs}_now_valid")
      case Inhibitor => wireMap(src"${lhs}_inhibitor")
      case Wren => wireMap(src"${lhs}_wren")
      case Chain => wireMap(src"${lhs}_chain")
      case Blank => wireMap(src"${lhs}")
      case DataOptions => wireMap(src"${lhs}_data_options")
      case ValidOptions => wireMap(src"${lhs}_valid_options")
      case ReadyOptions => wireMap(src"${lhs}_ready_options")
      case EnOptions => wireMap(src"${lhs}_en_options")
      case RVec => wireMap(src"${lhs}_rVec")
      case WVec => wireMap(src"${lhs}_wVec")
      case Retime => wireMap(src"${lhs}_retime")
      case SM => wireMap(src"${lhs}_sm")
      case Inhibit => wireMap(src"${lhs}_inhibit")
    }
  }

  def swap(lhs: => String, s: RemapSignal): String = {
    s match {
      case En => wireMap(src"${lhs}_en")
      case Done => wireMap(src"${lhs}_done")
      case Last => wireMap(src"${lhs}_last")
      case BaseEn => wireMap(src"${lhs}_base_en")
      case Mask => wireMap(src"${lhs}_mask")
      case Resetter => wireMap(src"${lhs}_resetter")
      case DatapathEn => wireMap(src"${lhs}_datapath_en")
      case CtrTrivial => wireMap(src"${lhs}_ctr_trivial")
      case IIDone => wireMap(src"${lhs}_II_done")
      case RstEn => wireMap(src"${lhs}_rst_en")
      case CtrEn => wireMap(src"${lhs}_ctr_en")
      case Ready => wireMap(src"${lhs}_ready")
      case Valid => wireMap(src"${lhs}_valid")
      case NowValid => wireMap(src"${lhs}_now_valid")
      case Inhibitor => wireMap(src"${lhs}_inhibitor")
      case Wren => wireMap(src"${lhs}_wren")
      case Chain => wireMap(src"${lhs}_chain")
      case Blank => wireMap(src"${lhs}")
      case DataOptions => wireMap(src"${lhs}_data_options")
      case ValidOptions => wireMap(src"${lhs}_valid_options")
      case ReadyOptions => wireMap(src"${lhs}_ready_options")
      case EnOptions => wireMap(src"${lhs}_en_options")
      case RVec => wireMap(src"${lhs}_rVec")
      case WVec => wireMap(src"${lhs}_wVec")
      case Retime => wireMap(src"${lhs}_retime")
      case SM => wireMap(src"${lhs}_sm")
      case Inhibit => wireMap(src"${lhs}_inhibit")
    }
  }

  def getLatency(sym: Exp[_], inReduce: Boolean) = {
    spatialConfig.target.latencyModel.latencyOf(sym, false)
  }
}
