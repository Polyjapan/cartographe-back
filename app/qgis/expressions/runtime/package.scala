package qgis.expressions

import qgis.expressions.Tree._

import scala.language.implicitConversions

package object runtime {
  sealed trait RuntimeValue {
    val value: Any

    def str: String = value.toString
  }
  case class IntValue(value: Int) extends RuntimeValue
  case class FloatValue(value: Float) extends RuntimeValue
  case class StringValue(value: String) extends RuntimeValue
  case class BooleanValue(value: Boolean) extends RuntimeValue
  case object NullValue extends RuntimeValue {
    override val value = "null"
  }
  case class ErrorValue(value: String) extends RuntimeValue {
    override def str: String = "ERROR: " + super.str
  }

  implicit def stringAsValue(s: String): RuntimeValue = StringValue(s)
  implicit def booleanAsValue(b: Boolean): RuntimeValue = BooleanValue(b)

  val intRegex = "-?[0-9]+".r // vulnerable to overflow exceptions
  val floatRegex = "-?[0-9]*(\\.[0-9]+([eE][-+]?[0-9]+)?|[eE][-+]?[0-9]+)".r // vulnerable to overflow exceptions

  def convertFromString(s: String): RuntimeValue = {
    if (s.nonEmpty) {
      if (intRegex.matches(s)) IntValue(s.toInt)
      else if (floatRegex.matches(s)) FloatValue(s.toFloat)
      else if (s.toLowerCase == "true") BooleanValue(true)
      else if (s.toLowerCase == "false") BooleanValue(false)
      else if (s.toLowerCase == "null") NullValue
      else StringValue(s)
    } else StringValue("")
  }

  def convertFromLiteral(lit: Literal[_]): RuntimeValue = lit match {
    case IntLiteral(v) => IntValue(v)
    case FloatLiteral(v) => FloatValue(v)
    case StringLiteral(v) => StringValue(v)
    case BooleanLiteral(v) => BooleanValue(v)
    case NullLiteral() => NullValue
  }

  def convertFromAny(anyVal: Any): RuntimeValue = anyVal match {
    case i: Int => IntValue(i)
    case f: Float => FloatValue(f)
    case s: String => StringValue(s)
    case b: Boolean => BooleanValue(b)
    case n: Unit => NullValue
    case null => NullValue
  }
}
