package qgis.expressions

import qgis.expressions.compile.Compiler

object QgsCompiler {
  def apply(name: String, src: String) = {
    print("Compiling script...")
    Lexer(src) match {
      case Right(tokens) =>
        println("Lexer output")
        println(tokens)

        Parser(tokens) match {
          case Right(tree) =>
            println("Parser output")
            println(tree)
            Some(Compiler.exportToJavascript(tree, name))
          case Left(error) =>
            println("Parse error:" + error)
            None
        }

      case Left(error) =>
        println("Lexer error:" + error)
        None
    }
  }
}
