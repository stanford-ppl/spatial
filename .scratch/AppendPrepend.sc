var x = List(1,3,4,5)

x +:= 6
println(x.mkString(", ")) // Prepended

x :+= 7
println(x.mkString(", ")) // Appended