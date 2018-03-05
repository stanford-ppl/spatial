package utils

trait Ctx {
  def file: String
  def line: Int
  def column: Int
  def content: Option[String]
}
