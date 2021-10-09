package qgis.expressions.runtime

import data.LayerDef.{Feature, TableDef}
import qgis.expressions.Tree._

// Better idea: compile to JS code directly?

object Runner {
  /*private def argAsBoolean(a: () => RuntimeValue) = {
    a() match {
      case b: BooleanValue => b
      case IntValue(i) => BooleanValue(i != 0)
      case FloatValue(f) => BooleanValue(f != 0)
      case NullValue => BooleanValue(false)
      case v: ErrorValue => v
      case other => ErrorValue("Expected boolean but got " + other)
    }
  }

  private def argAsNum(a: () => RuntimeValue) = {
    a() match {
      case a: IntValue => a
      case a: FloatValue => a
      case BooleanValue(b) => IntValue(if (b) 1 else 0)
      case v: ErrorValue => v
      case other => ErrorValue("Expected int-like but got " + other)
    }
  }

  private val binOps: Map[String, (() => RuntimeValue, () => RuntimeValue) => RuntimeValue] = {
    def mult(op: (() => RuntimeValue, () => RuntimeValue) => RuntimeValue, names: String*): Map[String, (() => RuntimeValue, () => RuntimeValue) => RuntimeValue] = {
      Map.from(names.map(name => name -> op))
    }

    def numericOp(name: String, intOp: (Int, Int) => AnyVal, floatOp: (Float, Float) => AnyVal): (String, (() => RuntimeValue, () => RuntimeValue) => RuntimeValue) = name -> ((a, b) => {
      convertFromAny((argAsNum(a), argAsNum(b)) match {
        case (IntValue(v1), IntValue(v2)) => intOp(v1, v2)
        case (IntValue(v1), FloatValue(v2)) => floatOp(v1, v2)
        case (FloatValue(v1), IntValue(v2)) => floatOp(v1, v2)
        case (FloatValue(v1), FloatValue(v2)) => floatOp(v1, v2)
        case (errorValue: ErrorValue, _) => errorValue
        case (_, errorValue: ErrorValue) => errorValue
        case _ => ErrorValue("Invalid input types for operator " + name)
      })
    })


    def and = {
      (a: () => RuntimeValue, b: () => RuntimeValue) => {
        argAsBoolean(a) match {
          case BooleanValue(aVal) =>
            if (aVal) argAsBoolean(b)
            else BooleanValue(false)
          case other => other
        }
      }
    }

    def or = {
      (a: () => RuntimeValue, b: () => RuntimeValue) => {
        argAsBoolean(a) match {
          case BooleanValue(aVal) =>
            if (!aVal) argAsBoolean(b)
            else BooleanValue(true)
          case other => other
        }
      }
    }

    Map(
      numericOp("+", _ + _, _ + _),
      numericOp("-", _ - _, _ - _),
      numericOp("*", _ * _, _ * _),
      numericOp("//", _ / _, _.toInt / _.toInt),
      numericOp("%", _ % _, _.toInt % _.toInt),
      numericOp("/", _.toFloat / _.toFloat, _ / _),
      numericOp("^", Math.pow(_, _).toInt, Math.pow(_, _).toFloat),
      numericOp("<", _ < _, _ < _),
      numericOp("<=", _ <= _, _ <= _),
      numericOp(">", _ > _, _ > _),
      numericOp(">=", _ >= _, _ >= _),
      "AND" -> and,
      "OR" -> or,
      "||" -> ((a: () => RuntimeValue, b: () => RuntimeValue) => StringValue(a().str + b().str))
    )
      .++(mult((a, b) => BooleanValue(a != b), "!=", "<>", "IS NOT"))
      .++(mult((a, b) => BooleanValue(a != b), "=", "IS"))

    // TODO: LIKE operators and regex (~)
  }

  def unaryOp(op: String, value: RuntimeValue): RuntimeValue = op match {
    case "NOT" => argAsBoolean(() => value) match {
      case BooleanValue(b) => BooleanValue(!b)
      case other => other // obviously an error
    }
    case _ => argAsNum(() => value) match {
      case IntValue(v) => IntValue(if (op == "-") -v else v)
      case FloatValue(v) => FloatValue(if (op == "-") -v else v)
      case other => other // obviously an error
    }
  }

  def run(expr: Expression, data: Data): RuntimeValue = expr match {
    case lit: Literal[_] => convertFromLiteral(lit)
    case Variable(vName) => ???
    case ColumnRef(vName) => ???
    case BinOp(op, lhs, rhs) =>
      val elhs = () => run(lhs, data)
      val erhs = () => run(rhs, data)

      binOps(op)(elhs, erhs)
    case UnaryOp(op, value) =>
      unaryOp(op, run(value, data))

    case InOp(needle, haystack, notIn) =>
      val eNeedle = run(needle, data)

      if (notIn) {
        val eHaystack = haystack.map(e => run(e, data)).toSet
        BooleanValue(!eHaystack(eNeedle))
      } else {
        haystack.foldLeft(false)((v, expr) => if (v) v else run(expr, data) == eNeedle)
      }

    case FunctionCall(name, args) => ???
    case ArrayAccess(name, args) => ???
    case NamedNode(name, args) => ???
    case WhenThen(name, args) => ???
    case IfThenElse(name, args) => ???
    case Error(msg) => ErrorValue(msg)
  }

  case class Data(tbl: TableDef, feature: Feature, featureData: Map[String, RuntimeValue], variables: Map[String, RuntimeValue]) {
    def +(pair: (String, RuntimeValue)): Data = copy(variables = this.variables + pair)

    def -(key: String): Data = copy(variables = this.variables - key)

    def apply(key: String): RuntimeValue = featureData.getOrElse(key, variables(key))
  }*/
}
