implicit class Quoting(sc: StringContext) {
  def r(args: Any*): Unit = {
    //val quoted = args.map(convertToText)
    println("Args: ")
    args.zipWithIndex.foreach{case (x,i) => println(s"  $i: $x") }
    println("Parts: ")
    sc.parts.zipWithIndex.foreach{case (x,i) => println(s"  $i: $x") }
  }
}

val x = List("a", "b", "c")

println(x.foldLeft(""){_+_})
println(x.foldRight(""){_+_})
