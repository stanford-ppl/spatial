package pcc.util

import java.io.File
import org.apache.commons.io.FileUtils

object files {
  def sep: String = java.io.File.separator
  def cwd: String = new java.io.File("").getAbsolutePath

  /**
    * Delete all files in the given path which end in the extension `ext`
    */
  def deleteExts(path: String, ext: String): Unit = {
    val files: Array[String] = Option(new File(path).list).map(_.filter(_.endsWith(ext))).getOrElse(Array.empty)
    files.foreach{filename =>
      val file = new File(path + java.io.File.separator + filename)
      file.delete()
    }
  }

  /**
    * Delete the given file (may be a directory)
    */
  def deleteFiles(file: File): Unit = FileUtils.deleteQuietly(file)

  /**
    * Copy the file at src to the dst path
    */
  def copyFile(src: String, dst: String): Unit = FileUtils.copyFile(new File(src), new File(dst))

  /**
    * Copy directory from source path to destination path
    */
  def copyDir(srcDir: File, dstDir: File): Unit = FileUtils.copyDirectory(srcDir, dstDir)
  def copyDir(srcDir: String, dstDir: String): Unit = copyDir(new File(srcDir), new File(dstDir))

}
