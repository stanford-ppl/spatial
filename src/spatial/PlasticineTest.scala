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

  protected def pirArgs:List[String] = 
    "bash" ::
    "run.sh" ::
    Nil

  def timer = System.getProperty("os.name") match {
    case "Mac OS X" => s"""gtime -f Runtime:%E"""
    case "Linux" => s"""/usr/bin/time -f Runtime:%E"""
  }

  abstract class PIRBackend(args:String="--pir --dot") extends Backend(name, args=args, "", "", "") {
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
      var cmd = pirArgs ++ args
      cmd ++= cmdlnArgs
      val timeout = 100000
      scommand(pass, cmd, timeout, parsepir _, RunError.apply)
    }

    def runpir() = {
      var cmd = pirArgs :+
      "--load=false" :+
      "--mapping=false" :+
      "--codegen=false"
      pirpass("runpir", cmd)
    }

    def mappir(args:String, fifo:Int=20) = {
      var cmd = pirArgs :+
      "--load=true" :+
      "--ckpt=1" :+
      "--mapping=true" :+
      s"--fifo-depth=$fifo" :+
      "--codegen=false" :+
      "--stat" 
      cmd ++= args.split(" ").map(_.trim).toList
      pirpass("mappir", cmd)
    }

    def genpsim(fifo:Int=20) = {
      var gentracecmd = pirArgs :+
      "--load=true" :+
      "--ckpt=0" :+
      "--codegen=true" :+
      "--end-id=5" :+
      //"--trace=true" :+
      "--psim=true" :+
      "--run-psim=false"
      gentracecmd ++= args.split(" ").map(_.trim).toList
      gentracecmd ++= cmdlnArgs
      var genpsimcmd = pirArgs :+
      "--load=true" :+
      "--ckpt=2" :+
      "--codegen=true" :+
      s"--fifo-depth=$fifo" :+
      "--psim=true" :+
      "--run-psim=false"
      genpsimcmd ++= args.split(" ").map(_.trim).toList
      genpsimcmd ++= cmdlnArgs
      val timeout = 3000
      //pirpass("gentrace", gentracecmd)
      pirpass("genpsim", genpsimcmd)
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

    def genruntst(args:String="", fifo:Int=20) = {
      var gentstcmd = pirArgs :+
      "--load=true" :+
      "--ckpt=2" :+
      "--codegen=true" :+
      s"--fifo-depth=$fifo" :+
      "--tungsten=true" :+
      "--psim=false" :+
      "--run-psim=false"
      gentstcmd ++= args.split(" ").map(_.trim).toList
      gentstcmd ++= cmdlnArgs
      val timeout = 3000
      scommand(s"gentst", gentstcmd, timeout, parsepir _, RunError.apply) >>
      scommand(s"maketst", "time make".split(" "), timeout, parseMake, MakeError.apply, wd=IR.config.genDir+"/tungsten") >>
      runtst(timeout=timeout)
    }

    def runtst(name:String="runtst", timeout:Int=6000) = {
      val runArg = runtimeArgs.cmds.headOption.getOrElse("")
      val res = scommand(name, s"$timer ./tungsten $runArg".split(" "), timeout=timeout, parseTst, RunError.apply, wd=IR.config.genDir+"/tungsten")
      res match {
        case Unknown => Pass
        case res => res
      }
    }

    def parseProute(vcLimit:Int)(line:String) = {
      val usedVc = if (line.contains("Used") && line.contains("VCs")) {
        Some(line.split("Used ")(1).split("VCs")(0).trim.toInt)
      } else None
      usedVc.fold[Result](Unknown) { usedVc =>
        if (vcLimit == 0 && usedVc > 0) Fail else Pass
      }
    }

    def runproute(
      row:Int=16, 
      col:Int=8, 
      vlink:Int=2, 
      slink:Int=4, 
      time:Int = -1, 
      iter:Int=1000, 
      vcLimit:Int=4, 
      stopScore:Int= -1,
      prefix:String="Top"
    ) = {
      val prouteHome = Source.fromFile(buildPath(sys.env("HOME"), s".pirconf")).getLines.flatMap { line =>
        if (line.contains("proute-home")) Some(line.split("proute-home=")(1).trim) else None
      }.toList.headOption.getOrElse(buildPath(IR.config.cwd, "pir", "plastiroute"))
      var cmd = s"${buildPath(prouteHome, "plastiroute")}" :: 
      "-n" :: s"node.csv" ::
      "-l" :: s"link.csv" ::
      "-v" :: s"summary.csv" ::
      "-g" :: s"proute.dot" ::
      "-G" :: s"final.place" ::
      "-T" :: "checkerboard" ::
      "-a" :: "route_min_directed_valient" ::
      "-r" :: s"$row" ::
      "-c" :: s"$col" ::
      "-x" :: s"$vlink" ::
      "-e" :: s"$slink" ::
      "-S" :: s"$time" ::
      "-q" :: s"$vcLimit" ::
      "-E" :: s"$stopScore" ::
      "-s0" ::
      s"-i$iter" ::
      Nil
      if (prefix != "") cmd :+= s"-X$prefix"
      cmd ++= "-p100 -t1 -d100".split(" ").map(_.trim).toList
      val timeout = 10800 * 2 // 6 hours
      val name = "runproute"
      cmd = timer.split(" ").toList ++ cmd
      scommand(name, cmd, timeout, parseProute(vcLimit) _, RunError.apply, wd=IR.config.genDir+"/plastisim")
    }

    def parsePsim(line:String) = {
      if (line.contains("Simulation complete at cycle:")) {
        println(line)
        Pass
      }
      else if (line.contains("DEADLOCK") || line.contains("TIMEOUT")) Fail
      else Unknown
    }

    def runpsim(name:String="runpsim", flit:Int=512, linkTp:String="B", placefile:String="") = {
      var cmd = s"${buildPath(IR.config.cwd, "pir", "plastisim", "plastisim")}" :: 
      "-f" :: s"${IR.config.genDir}/plastisim/psim.conf" ::
      s"-i$flit" ::
      "-l" :: s"$linkTp" ::
      "-q1" ::
      Nil
      if (placefile != "") {
        cmd :+= "-p"
        cmd :+= placefile
      }
      val timeout = 10800 * 4 // 12 hours
      scommand(name, cmd, timeout, parsePsim _, RunError.apply)
    }

    def psh(ckpt:String) = {
      var cmd = pshPath ::
      s"--ckpt=$ckpt" ::
      Nil
      val timeout = 600
      command(s"psh", cmd, timeout, { case _ => Unknown}, RunError.apply)
    }

  }

  object Asic extends PIRBackend {
    def runPasses():Result = {
      val runArg = runtimeArgs.cmds.headOption.getOrElse("")
      genpir() >>
      pirpass("gentst", s"--net=asic".split(" ").toList) >>
      scommand(s"maketst", s"$timer make".split(" "), timeout=6000, parseMake, MakeError.apply, wd=IR.config.genDir+"/tungsten") >>
      scommand(s"idealroute", s"$timer python ../tungsten/bin/idealroute.py -l link.csv -p ideal.place -i /Top/idealnet".split(" "), timeout=10, parseMake, MakeError.apply, wd=IR.config.genDir+"/plastisim") >>
      scommand(s"cpp2p", s"cp script_p2p script".split(" "), timeout=10, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      runtst("runp2p")
    }
  }

  case object P2PNoSim extends PIRBackend {
    val row:Int=14
    val col:Int=14
    override def shouldRun: Boolean = super.shouldRun || checkFlag(s"test.P2PNoSim")
    override val name = s"P2PNoSim"
    def runPasses():Result = {
      genpir() >>
      runpir() >>
      mappir(s"--net=p2p --row=$row --col=$col")
    }
  }

  case class P2P(
    row:Int=16,
    col:Int=8,
  ) extends PIRBackend {
    override def shouldRun: Boolean = super.shouldRun || checkFlag(s"test.P2P")
    override val name = s"P${row}x${col}"
    def runPasses():Result = {
      var mapres = genpir() >>
      runpir() >>
      mappir(s"--net=p2p --row=$row --col=$col") 
      val psimres = mapres >> genpsim() >> runpsim()
      //val tstres = mapres >> genruntst()
      psimres //>> tstres
    }
  }

  case class Hybrid(
    row:Int=16,
    col:Int=8,
    vlink:Int=2,
    slink:Int=4,
    time:Int= -1,
    scheduled:Boolean=false,
    iter:Int=1000,
    vcLimit:Int=4,
    linkTp:String="B",
    flit:Int=512,
    fifo:Int=100,
  ) extends PIRBackend {
    override def shouldRun: Boolean = super.shouldRun || checkFlag(s"test.Hybrid")
    override val name = {
      var n = s"H${row}x${col}v${vlink}s${slink}"
      if (time != -1) n += s"t${time}"
      if (vcLimit != 4) n += s"c${vcLimit}"
      if (scheduled) n += s"w"
      if (linkTp != "B") n += s"${linkTp}"
      if (flit != 512) n += s"f${flit}"
      n
    }
    def runPasses():Result = {
      genpir() >>
      runpir() >>
      mappir(s"--row=$row --col=$col --pattern=checkerboard --vc=$vcLimit --scheduled=${scheduled}", fifo=fifo) >>
      genpsim() >>
      runproute(row=row, col=col, vlink=vlink, slink=slink, iter=iter, vcLimit=vcLimit) >>
      runpsim(placefile=s"${IR.config.genDir}/plastisim/final.place", linkTp=linkTp, flit=flit)
    }
  }

  case class Static(
    row:Int=16,
    col:Int=8,
    vlink:Int=2,
    slink:Int=4,
    time:Int= -1,
    scheduled:Boolean=false,
    iter:Int=1000,
    linkTp:String="B",
    fifo:Int=100,
  ) extends PIRBackend {
    override def shouldRun: Boolean = super.shouldRun || checkFlag(s"test.Static")
    override val name = {
      var n = s"S${row}x${col}v${vlink}s${slink}"
      if (time != -1) n += s"t${time}"
      if (scheduled) n += s"w"
      if (linkTp != "B") n += s"${linkTp}"
      n
    }
    def runPasses():Result = {
      genpir() >>
      runpir() >>
      mappir(s"--row=$row --col=$col --pattern=checkerboard --vc=0 --scheduled=${scheduled}", fifo=fifo) >>
      genpsim() >>
      runproute(row=row, col=col, vlink=vlink, slink=slink, iter=iter, vcLimit=0, stopScore=25) >>
      runpsim(placefile=s"${IR.config.genDir}/plastisim/final.place", linkTp=linkTp)
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
    vlink:Int = 3,
    slink:Int = 4,
    iter:Int = 300,
    vcLimit:Int = 4,
    module:Boolean = false,
  ) extends PIRBackend {
    override val name = if (module) "MDTst" else "Tst"
    private val genName = name + "_" + property("project").getOrElse("")
    override def genDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/"
    override def logDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/log"
    override def repDir(name:String):String = s"${IR.config.cwd}/gen/${this.genName}/$name/report"
    val runhybrid = checkFlag("hybrid")
    def runPasses():Result = {
      val runArg = runtimeArgs.cmds.headOption.getOrElse("")
      genpir() >>
      pirpass("gentst", s"${if (module) "--module" else ""} --mapping=true --codegen=true --net=hybrid --tungsten --psim=false --row=$row --col=$col".split(" ").toList) >>
      (if (module) scommand(s"gen_link", s"$timer python ../tungsten/bin/gen_link.py -p extlink.csv -d link.csv".split(" "), timeout=10, parseMake, MakeError.apply, wd=IR.config.genDir+"/plastisim") else Pass) >>
      scommand(s"maketst", s"$timer make".split(" "), timeout=6000, parseMake, MakeError.apply, wd=IR.config.genDir+"/tungsten") >>
      scommand(s"idealroute", s"$timer python ../tungsten/bin/idealroute.py -l link.csv -p ideal.place -i ${if (module) "" else "/Top"}/idealnet".split(" "), timeout=10, parseMake, MakeError.apply, wd=IR.config.genDir+"/plastisim") >>
      scommand(s"cpp2p", s"cp script_p2p script".split(" "), timeout=10, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      runtst("runp2p", timeout=1000000) >>
      scommand(s"p2pstat", s"python bin/simstat.py".split(" "), timeout=10, parseRunError, RunError.apply, wd=IR.config.genDir+"/tungsten") >>
      (if (runhybrid)
      runproute(row=row, col=col, vlink=vlink, slink=slink, iter=iter, vcLimit=vcLimit, prefix=if(module)"" else "Top") >>
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
    Asic +:
    P2PNoSim +:
    Dot +:
    Tst() +:
    Tst(module=true) +:
    SpatialOnly +:
    super.backends

}
