package utils.implicits

trait Readable {
  def readable: String = this.toString
}

object Readable {

  private def readable(x: Any): String = x match {
    case c: Class[_] => c.getName.split('$').last.replace("class ", "").replace("package","").split('.').filterNot(_ == "").mkString(".")
    case r: Readable => r.readable
    case x: AnyRef =>
      if (x eq null) "null" else x.toString
    case _ => x.toString
  }

  implicit class ReadablePrinter(sc: StringContext) {
    def r(args: Any*): String = sc.raw(args.map(readable): _*).stripMargin
  }
}
