package spatial.lang
package control

class NamedClass(name: String) extends Directives(CtrlOpt(Some(name),None,None,None)) {
  lazy val Accel = new AccelClass(Some(name))
  lazy val Pipe = new Pipe(Some(name), ii = None, directive = None)
  lazy val Stream = new Stream(Some(name), None)
  lazy val Sequential = new Sequential(Some(name), None)
}
