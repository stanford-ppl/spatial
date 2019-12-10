package spatial

import spatial._
import argon.DSLTest
import spatial.util.spatialConfig
import utils.io.files._
import scala.reflect.runtime.universe._
import scala.io.Source

trait PlasticineTest extends DSLTest { test =>

  protected val cmdlnArgs = sys.env.get("TEST_ARGS").getOrElse("").split(" ").map(_.trim).toList

  protected val pshPath = buildPath(IR.config.cwd, "pir", "bin", "psh")

  protected def pirArgs:String = "bash run.sh"

  protected def pirArgList:List[String] = pirArgs.split(" ").toList

  def timer = System.getProperty("os.name") match {
    case "Mac OS X" => s"""gtime -f Runtime:%E"""
    case "Linux" => s"""/usr/bin/time -f Runtime:%E"""
  }

  abstract class PIRBackend(args:String="--pir") extends Backend(name, args=args, "", "", "") {
    override val makeTimeout: Long = 12000 // Timeout for compiling, in seconds
    override val name = this.getClass.getSimpleName.replace("$","")
    override def shouldRun: Boolean = checkFlag(s"test.${name}") || checkFlag(s"test.PIR")
    def compileOnly = checkFlag(s"test.compileOnly")
    def runOnly = checkFlag(s"test.runOnly")
    override def genDir(name:String):String = s"${IR.config.cwd}/gen/${this.name}/$name/"
    override def logDir(name:String):String = s"${IR.config.cwd}/gen/${this.name}/$name/log"
    override def repDir(name:String):String = s"${IR.config.cwd}/gen/${this.name}/$name/report"
    override def runBackend() = {
      s"${test.name}" should s"run for backend $name" in {
        val name = test.name
        IR.config.name = name
        IR.config.genDir = genDir(test.name)
        IR.config.logDir = logDir(test.name)
        IR.config.repDir = repDir(test.name)
        createDirectories(IR.config.genDir)
        createDirectories(IR.config.logDir)
        createDirectories(IR.config.repDir)
        val result = runPasses()
        result.resolve()
      }
    }
    def runPasses():Result

    implicit class ResultOp(result:Result) {
      def >> (next: => Result) = result match {
        case Pass => next
        case result => result.orElse(next)
      }
    }

    def scommand(pass: String, args: Seq[String], timeout: Long, parse: String => Result, Error: String => Result, wd:String=IR.config.genDir): Result = {
      import java.nio.file.{Paths, Files}
      val logPath = IR.config.logDir + s"/$pass.log"
      var res:Result = Unknown
      var rerunPass = false
      rerunPass |= checkFlag(s"rerun.${pass}") || checkFlag(s"rerun.all")
      if (Files.exists(Paths.get(logPath)) && !rerunPass) {
        res = scala.io.Source.fromFile(logPath).getLines.map { parse }.fold(res){ _ orElse _ }
      }
      if (res == Pass) {
        println(s"${Console.GREEN}${logPath}${Console.RESET} succeeded. Skipping")
        Pass 
      } else command(pass, args, timeout, parse, Error, wd)
    }

    def genpir():Result = {
      import java.nio.file.{Paths, Files}
      val pirPath = IR.config.genDir + "/pir/AccelMain.scala"
      val pirExists = Files.exists(Paths.get(pirPath))
      val buildExists = Files.exists(Paths.get(IR.config.genDir + "/build.sbt"))
      val rerunPass = checkFlag(s"rerun.genpir") || checkFlag(s"rerun.all")
      if (pirExists && buildExists && !rerunPass) {
        println(s"${Console.GREEN}${pirPath}${Console.RESET} succeeded. Skipping")
        Pass 
      } else {
        compile().next()()
      }
    }

    def parsepir(line:String) = {
      if (line.contains("failed dot")) Unknown
      else if (line.contains("error")) Fail
      else if (line.contains("fail")) Fail
      else if (line.contains("Compilation succeed in")) Pass
      else Unknown
    }

    def pirpass(pass:String, args:List[String]) = {
      var cmd = pirArgList ++ args
      cmd ++= cmdlnArgs
      val timeout = 100000
      scommand(pass, cmd, timeout, parsepir _, RunError.apply)
    }

    def runpir() = {
      var cmd = pirArgList :+
      "--load=false" :+
      "--mapping=false" :+
      "--codegen=false"
      pirpass("runpir", cmd)
    }

    def mappir(args:String, fifo:Int=20) = {
      var cmd = pirArgList :+
      "--load=true" :+
      "--ckpt=1" :+
      "--mapping=true" :+
      s"--fifo-depth=$fifo" :+
      "--codegen=false" :+
      "--stat" 
      cmd ++= args.split(" ").map(_.trim).toList
      pirpass("mappir", cmd)
    }

    def parseMake(line:String) = {
      if (line.contains("error") || line.contains("exception")) Fail
      else if (line.contains("Runtime")) Pass
      else Unknown
    }

    def parseTst(line:String) = {
      if (line.contains("Simulation complete at cycle")) {
        println(line)
        Unknown
      } else if (line.contains("PASS: true")) {
        println(line)
        Pass
      } else if (line.contains("PASS: false")) {
        println(line)
        Fail
      } else if (line.contains("exception")) {
        println(line)
        Fail
      } 
      else Unknown
    }

    def runtst(name:String="runtst", timeout:Int=6000) = {
      val runArg = runtimeArgs.cmds.headOption.getOrElse("")
      val res = scommand(name, s"$timer ./tungsten $runArg".split(" "), timeout=timeout, parseTst, RunError.apply, wd=IR.config.genDir+"/tungsten")
      res match {
        case Unknown => Pass
        case res => res
      }
    }

    def parseProute(vcLimit:Int=4)(line:String) = {
      val usedVc = if (line.contains("Used") && line.contains("VCs")) {
        Some(line.split("Used ")(1).split("VCs")(0).trim.toInt)
      } else None
      usedVc.fold[Result](Unknown) { usedVc =>
        if (vcLimit == 0 && usedVc > 0) Fail else Pass
      }
    }

    def psh(ckpt:String) = {
      var cmd = pshPath ::
      s"--ckpt=$ckpt" ::
      Nil
      val timeout = 600
      command(s"psh", cmd, timeout, { case _ => Unknown}, RunError.apply)
    }

  }

  case object Dot extends Backend(
    name="Dot", 
    args="--sim --dot",
    make="",
    run="",
    model=""
  ) {
    override def shouldRun: Boolean = checkFlag(s"test.Dot")
    override def runBackend() = {
      s"${test.name}" should s"run for backend $name" in {
        (compile().next()() match {
          case Unknown => Pass
          case res => res
        }).resolve()
      }
    }
  }

  case class Tst(
    row:Int=20,
    col:Int=20,
    module:Boolean = false,
  ) extends PIRBackend {
    override val name = if (module) "MDTst" else "Tst"
    private val genName = name + "_" + property("project").getOrElse("")
    override def genDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/"
    override def logDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/log"
    override def repDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/report"
    val runhybrid = checkFlag("hybrid")
    val fast = checkFlag("fast")
    def runPasses():Result = {
      val runArg = runtimeArgs.cmds.headOption.getOrElse("")
      genpir() >>
      pirpass("gentst", s"${if (module) "--module" else ""} --mapping=true --codegen=true --net=hybrid --tungsten --psim=false --row=$row --col=$col".split(" ").toList) >>
      (if (module) scommand(s"gen_link", s"$timer python ../tungsten/bin/gen_link.py -p extlink.csv -d link.csv".split(" "), timeout=10, parseMake, MakeError.apply, wd=IR.config.genDir+"/plastisim") else Pass) >>
      scommand(s"maketst", s"$timer make ideal ${if (fast) "DEBUG=1" else "DEBUG=0"}".split(" "), timeout=6000, parseMake, MakeError.apply, wd=IR.config.genDir+"/tungsten") >>
      runtst("runp2p", timeout=1000000) >>
      scommand(s"p2pstat", s"python3 bin/simstat.py".split(" "), timeout=10, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      //scommand(s"p2panot", s"python3 bin/annotate.py".split(" "), timeout=30, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      (if (runhybrid)
      scommand(s"runproute", s"$timer make proute".split(" "), timeout=10800 * 2, parseProute()(_), MakeError.apply, wd=IR.config.genDir+"/tungsten") >>
      scommand(s"cphybrid", s"cp script_hybrid script".split(" "), timeout=10, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      runtst("runhybrid", 1000000)
      else Pass
      )
    }
  }

  case object SpatialOnly extends PIRBackend {
    override def genDir(name:String):String = s"${IR.config.cwd}/gen/$name/"
    override def logDir(name:String):String = s"${IR.config.cwd}/gen/$name/log"
    override def repDir(name:String):String = s"${IR.config.cwd}/gen/$name/report"
    def runPasses():Result = {
      genpir() match {
        case Unknown => Pass
        case res => res
      }
    }
  }

  override def backends: Seq[Backend] = 
    Dot +:
    Tst() +:
    Tst(module=true) +:
    SpatialOnly +:
    super.backends

}
