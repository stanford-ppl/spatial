package emul

import scala.reflect.ClassTag

class StreamIn[T](name: String, elemFromString: String => T) extends scala.collection.mutable.Queue[T] {
  def initMem(): Unit = {
    print(s"Enter name of file to use for StreamIn $name: ")
    val filename = scala.io.StdIn.readLine()
    try {
      val source = scala.io.Source.fromFile(filename)
      source.getLines.foreach{line =>
        if (line.exists(_.isDigit)) {
          val elem = elemFromString(line)
          this.enqueue(elem)
        }
      }
    }
    catch {case e: Throwable =>
      println("There was a problem while opening the specified file for reading.")
      println(e.getMessage)
      e.printStackTrace()
      sys.exit(1)
    }
  }
}

class StreamOut[T](name: String, elemToString: T => String) extends scala.collection.mutable.Queue[T] {
  var writer: java.io.PrintWriter = null

  def initMem(): Unit = {
    print(s"Enter name of file to use for StreamOut $name:")
    try {
      val filename = scala.io.StdIn.readLine()
      writer = new java.io.PrintWriter(new java.io.File(filename))
    }
    catch { case e: Throwable =>
      println("There was a problem while opening the specified file for writing.")
      println(e.getMessage)
      e.printStackTrace()
      sys.exit(1)
    }
  }

  def dump(): Unit = {
    this.foreach{elem =>
      val line = elemToString(elem)
      writer.println(line)
    }
    writer.close()
  }
}

class BufferedOut[T:ClassTag](name: String, size: Int, zero: T, elemToString: T => String) {
  val data = Array.fill[T](size)(zero)
  var writer: java.io.PrintWriter = null

  def initMem(): Unit = {
    print(s"Enter name of file to use for BufferedOut $name: ")
    try {
      val filename = scala.io.StdIn.readLine()
      writer = new java.io.PrintWriter(new java.io.File(filename))
    }
    catch{ case e: Throwable =>
      println("There was a problem while opening the specified file for writing.")
      println(e.getMessage)
      e.printStackTrace()
      sys.exit(1)
    }
  }

  def dump(): Unit = {
    data.foreach{elem =>
      val line = elemToString(elem)
      writer.println(line)
    }
  }

  def close(): Unit = writer.close()
}

