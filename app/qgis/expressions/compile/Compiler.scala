package qgis.expressions.compile

import data.LayerDef.{Feature, TableDef}
import qgis.expressions.Tree._

import scala.collection.mutable

// Style.ts

object Compiler {
  def unaryOp(op: String, v: String): String = op match {
    case "NOT" => f"!($v)"
    case "-" => f"-($v)"
    case _ => v
  }

  def exportToJavascript(expr: Expression, funcName: String): String = {
    val tlvars = mutable.Map[String, String]()
    val body = compile(expr, true)(tlvars)

    val varDeclarations = tlvars.map(pair => "var " + pair._1 + " = (" + pair._2 + ");\n").mkString("\n")

    f"""function $funcName(feature) {
       |  $varDeclarations
       |  $body
       |}""".stripMargin
  }

  private def binOp(op: String, a: String, b: String) = {
    def nullCmp(a: String, eq: Boolean) = {
      if (eq) f"""(!($a) || ($a) === "null" || ($a) === "undefined")"""
      else f"""(($a) && ($a) !== "null" && ($a) !== "undefined")"""
    }

    val identical = Set("+", "-", "*", "%", "/", "!=", "<", ">", "<=")
    val remap = Map("=" -> "===", "<>" -> "!=", "!=" -> "!==", "AND" -> "&&",
      "OR" -> "||", "||" -> "+")
    val func = Map(
      "//" -> ((a: String, b: String) => (f"Math.floor(($a) / ($b))")),
      "^" -> ((a: String, b: String) => (f"Math.pow(($a), ($b))")),
      "IS" -> ((a: String, b: String) => if (a == "null") nullCmp(b, true) else if (b == "null") nullCmp(a, true) else f"($a) == ($b)"),
      "IS NOT" -> ((a: String, b: String) => if (a == "null") nullCmp(b, false) else if (b == "null") nullCmp(a, false) else f"($a) != ($b)"),
    )

    if (identical(op)) s"($a) $op ($b)"
    else if (remap.keySet(op)) s"($a) ${remap(op)} ($b)"
    else if (func.keySet(op)) func(op)(a, b)
    else "undefined"
  }

  private def ret(v: String)(implicit topLevel: Boolean): String = {
    if (topLevel) f"return $v;"
    else v
  }

  private def compile(expr: Expression, topLevel: Boolean = false)(implicit vars: mutable.Map[String, String]): String = {
    implicit val tl: Boolean = topLevel
    expr match {
      case lit: Literal[_] =>
        ret(lit match {
          case IntLiteral(v) => f"$v"
          case FloatLiteral(v) => f"$v"
          case BooleanLiteral(v) => f"$v"
          case StringLiteral(v) => f""""$v""""
          case NullLiteral() => "null"
        })
      case Variable(vName) => ret(vName)
      case ColumnRef(vName) => ret(f"""feature.get("$vName")""")
      case BinOp(op, lhs, rhs) => ret(binOp(op, compile(lhs), compile(rhs)))
      case UnaryOp(op, value) => ret(unaryOp(op, compile(value)))
      case InOp(needle, haystack, notIn) =>
        val eNeedle = compile(needle)
        val stack = "([" + haystack.map(exp => f"(${compile(exp)})").mkString(",") + "])"

        val base = f"$stack.indexOf($eNeedle)"

        if (notIn) ret(base + " === -1")
        else ret(base + " !== -1")
      case FunctionCall(name, args) =>
        val compiledArgs = args.map(a => compile(a)).map(a => f"($a)").mkString(", ")

        if (name == "var" && args.length == 1) {
          ret(f"($compiledArgs)")
        } else ret(f"qgs_builtins_$name([$compiledArgs], feature)")

      case ArrayAccess(name, arg) =>
        val compiledArg = compile(arg)
        val compiledOrig = compile(name)
        ret(f"($compiledOrig)[$compiledArg]")
      case NamedNode(name, args) =>
        vars.put(name, compile(args))

        ret(name)
      case IfThenElse(iffs, elze) =>
        val iffsMap = iffs.map(wt => (compile(wt.when), compile(wt.thenn)))
        val elzeComp = elze.map(a => compile(a)).getOrElse("undefined")

        ret { iffsMap.foldRight(elzeComp)((cond, elze) => f"(${cond._1} ? (${cond._2}) : $elze)") }

      case Error(msg) => ret { "undefined" };
    }
  }
}
