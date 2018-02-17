package pcc.poly

sealed abstract class ConstraintType { def toInt: Int }
object ConstraintType {
  case object GEQ_ZERO extends ConstraintType { def toInt = 1; override def toString = "1" }
  case object EQL_ZERO extends ConstraintType { def toInt = 0; override def toString = "0" }
}