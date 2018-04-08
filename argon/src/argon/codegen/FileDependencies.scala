package argon
package codegen

import utils.io.files

trait FileDependencies extends Codegen {
  var dependencies: List[CodegenDep] = Nil

  lazy val files_list: Seq[String] = {
    io.Source.fromURL(getClass.getResource("/files_list")).mkString("").split("\n")
  }

  sealed trait CodegenDep {
    def copy(out: String): Unit
  }

  case class FileDep(folder: String, name: String, relPath: String = "", outputPath:Option[String] = None) extends CodegenDep {
    def copy(out: String): Unit = {
      val outPathApp = outputPath.getOrElse(name)
      val relPathApp = relPath + outPathApp
      try {
        files.copyResource(s"/$folder/$name", s"$out/$relPathApp")
      }
      catch {case t: Throwable =>
        bug(s"Error $t")
        bug(s"Cannot copy dependency:")
        bug("  src: " + folder + "/" + name)
        bug("  dst: " + out + relPathApp)
        throw t
      }
    }
  }

  case class DirDep(folder: String, name: String, relPath: String = "", outputPath:Option[String] = None) extends CodegenDep {
    override def copy(out: String): Unit = {
      val dir = folder + "/" + name
      // Console.println("Looking at " + dir)

      def rename(e: String) = {
        val srcFolder = if (folder.startsWith("./")) folder.split("/") else {"./" + folder}.split("/")
        val path = e.split("/").drop(srcFolder.length)
        if (outputPath.isDefined) {
          val sourceName = folder + "/" + path.dropRight(1).mkString("/")
          val outputName = outputPath.get + path.last
          FileDep(sourceName, path.last, relPath, Some(outputName))
        }
        else {
          val outputName = path.mkString("/")
          FileDep(folder, outputName, relPath)
        }
      }

      files_list.filter(_.startsWith("./"+dir))
                .map{d => rename(d)}
                .foreach{f => f.copy(out)}
    }
  }

  def copyDependencies(out: String): Unit = {
    dependencies.foreach{dep => dep.copy(out) }
  }

  override protected def postprocess[R](b: Block[R]): Block[R] = {
    copyDependencies(out)
    super.postprocess(b)
  }
}
