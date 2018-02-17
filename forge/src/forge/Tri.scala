package forge

object Tri {
  sealed abstract class Tri[+A,+B] extends Product with Serializable {
    def isNix: Boolean = isEmpty
    def isOne: Boolean
    def isTwo: Boolean
    def isEmpty: Boolean
    def isDefined: Boolean = !isEmpty
    def get: Either[A,B]
    def getOne: Option[A]
    def getTwo: Option[B]
  }

  case class One[+A,+B](value: A) extends Tri[A,B] {
    final override def isOne: Boolean = true
    final override def isTwo: Boolean = false
    final override def isEmpty: Boolean = false
    final override def get: Either[A,B] = Left(value)
    final override def getOne: Option[A] = Some(value)
    final override def getTwo: Option[B] = None
  }
  case class Two[+A,+B](value: B) extends Tri[A,B] {
    final override def isOne: Boolean = false
    final override def isTwo: Boolean = true
    final override def isEmpty: Boolean = false
    final override def get: Either[A,B] = Right(value)
    final override def getOne: Option[A] = None
    final override def getTwo: Option[B] = Some(value)
  }
  case object Nix extends Tri[Nothing,Nothing] {
    final override def isOne: Boolean = false
    final override def isTwo: Boolean = false
    final override def isEmpty: Boolean = true
    final override def get: Either[Nothing, Nothing] = throw new NoSuchElementException("Nix.get")
    final override def getOne: Option[Nothing] = None
    final override def getTwo: Option[Nothing] = None
  }
}

