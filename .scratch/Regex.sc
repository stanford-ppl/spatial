import scala.util.Try

object FRequest {
  lazy val regex = raw"FRequest ([0-9]*) (.*)".r
  def unapply(x: String): Option[(Int,String)] = x match {
    case regex(n, file) => Try(n.toInt).toOption.map{n => (n,file) }
    case _ => None
  }
}

val x = "FRequest 2000 hello"

x match {
  case FRequest(n, file) => println(s"$n: $file")
  case _ => println("nope")
}