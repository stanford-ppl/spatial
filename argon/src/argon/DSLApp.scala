package argon


trait DSLApp extends Compiler {

  final def main(args: Array[String]): Unit = compile(args)

}
