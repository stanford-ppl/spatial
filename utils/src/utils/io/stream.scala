package utils.io

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.file.{Files, Paths}

object stream {
  def createStream(dir: String, filename: String): PrintStream = {
    Files.createDirectories(Paths.get(dir))
    val fname = dir + files.sep + filename
    val outputStream = new BufferedOutputStream(new FileOutputStream(fname), 1 << 16)
    new PrintStream(outputStream)
  }
}
