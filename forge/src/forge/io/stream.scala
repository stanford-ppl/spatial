package forge.io

import java.io.PrintStream
import java.nio.file.{Files,Paths}

object stream {
  def createStream(dir: String, filename: String): PrintStream = {
    Files.createDirectories(Paths.get(dir))
    new PrintStream(dir + files.sep + filename)
  }
}
