package qgis.expressions
import scala.util.parsing.input.Positional

// https://github.com/qgis/QGIS/blob/23e232317667f9ccb50f53638f0c01627469664d/src/core/qgsexpressionparser.yy

object Tokens {

  class Token(name: String) extends Positional {
    override def toString: String = name
  }

  trait Literal

  case class Whitespace() extends Token("")

  case class Symbol(symb: String) extends Token(symb)
  case class Variable(name: String) extends Token(s"variable '${name}'")
  case class SpecialColumn(name: String) extends Token(s"special column '${name}'")
  case class Identifier(name: String) extends Token(s"identifier '${name}'")
  case class QuotedIdentifier(name: String) extends Token(s"identifier '${name}'")
  case class NamedNode(name: String) extends Token("named node " + name)

  case class Keyword(kw: String) extends Token(kw)
  case class StringLiteral(value: String) extends Token(s"string '${value}'") with Literal
  case class IntLiteral(value: Int) extends Token(s"int '$value'") with Literal
  case class FloatLiteral(value: Float) extends Token(s"float '$value'") with Literal
  case class BooleanLiteral(value: Boolean) extends Token(s"bool '$value'") with Literal
  case class NullLiteral() extends Token("null") with Literal
  case class LPar() extends Token("(")
  case class RPar() extends Token(")")
  case class LBrack() extends Token("[")
  case class RBrack() extends Token("]")
  case class Comma() extends Token(",")

  sealed trait ConditionalToken
  case class Case() extends Token("CASE") with ConditionalToken
  case class When() extends Token("WHEN") with ConditionalToken
  case class Then() extends Token("THEN") with ConditionalToken
  case class Else() extends Token("ELSE") with ConditionalToken
  case class End() extends Token("END") with ConditionalToken

}
