package utils.io

import java.io.{File, PrintStream}
import java.nio.file._
import java.util.function.Consumer

import scala.io.Source

object files {
  def sep: String = java.io.File.separator
  def cwd: String = new java.io.File("").getAbsolutePath

  /**
    * Delete all files in the given path which end in the extension `ext`.
    * If recursive is true, recursively delete files with this extension.
    */
  def deleteExts(path: String, ext: String, recursive: Boolean = false): Unit = {
    val files: Array[String] = Option(new File(path).list).getOrElse(Array.empty)
    files.foreach{filename =>
      val file = new File(path + java.io.File.separator + filename)
      if (file.isDirectory && recursive) {
        deleteExts(filename, ext, recursive)
      }
      else if (filename.endsWith("."+ext)) {
        file.delete()
      }
    }
  }

  def deleteDirectory(file: File): Unit = {
    for (file <- file.listFiles) deleteFiles(file)
  }

  /**
    * Delete the given file (may be a directory)
    */
  def deleteFiles(file: File): Unit = {
    if (file.isDirectory) deleteDirectory(file)
    if (file.exists) file.delete()
  }

  /**
    * Copy the file at src to the dst path
    */
  def copyFile(src: String, dst: String): Unit = {
    if (src == dst) throw new Exception(s"Source file $src and destination are the same.")
    val srcFile = new File(src)
    val dstFile = new File(dst)
    if (dstFile.exists() && !dstFile.canWrite) throw new Exception(s"Destination $dst exists and cannot be written.")
    if (!srcFile.exists()) throw new Exception(s"Source for copy $src does not exist.")

    Files.copy(Paths.get(src), Paths.get(dst))
  }

  /**
    * Copy directory from source path to destination path
    */
  def copyDir(srcDir: String, dstDir: String): Unit = {
    if (srcDir == dstDir) throw new Exception(s"Source file $srcDir and destination are the same.")

    val srcPath = Paths.get(srcDir)

    object Copier extends Consumer[Path] {
      override def accept(t: Path): Unit = {
        val b = Paths.get(dstDir, t.toString.substring(srcDir.length()))
        Files.copy(t, b, StandardCopyOption.REPLACE_EXISTING)
      }
    }

    Files.walk(srcPath).forEach(Copier)
  }

  /**
    * Copy the resource file to the given destination
    */
  def copyResource(src: String, dest: String): Unit = {
    val outFile = new File(dest)
    val outPath = new File(dest.split("/").dropRight(1).mkString("/"))
    outPath.mkdirs()
    val out = new PrintStream(outFile)
    val res = getClass.getResourceAsStream(src)
    Source.fromInputStream(res).getLines().foreach{line => out.println(line) }
    out.close()
    res.close()
  }

}
