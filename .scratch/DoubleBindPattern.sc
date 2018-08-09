val x = 1
val y = 2
val z = 1


def checkForMatch(x: Int, y: Int): Boolean = (x,y) match {
  //case (a,`a`) => true
  //case (a,a) => true
  case (a,b) if a == b => true
  case _ => false
}

checkForMatch(x,y)
checkForMatch(x,z)