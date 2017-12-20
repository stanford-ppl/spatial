package pcc.util

import java.io.File

object files {
  def sep: String = java.io.File.separator
  def cwd: String = new java.io.File("").getAbsolutePath

  def deleteExts(path: String, ext: String): Unit = {
    val files: Array[String] = Option(new File(path).list).map(_.filter(_.endsWith(ext))).getOrElse(Array.empty)
    files.foreach{filename =>
      val file = new File(path + java.io.File.separator + filename)
      file.delete()
    }
  }
}
