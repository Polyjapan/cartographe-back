package qgis.expressions

import scala.util.parsing.input.Positional

object Tree {
  sealed trait Expression extends Positional

  sealed trait Literal[T] extends Expression {
    val value: T
  }
  case class IntLiteral(override val value: Int) extends Literal[Int]
  case class FloatLiteral(override val value: Float) extends Literal[Float]
  case class StringLiteral(override val value: String) extends Literal[String]
  case class BooleanLiteral(override val value: Boolean) extends Literal[Boolean]
  case class NullLiteral() extends Literal[Unit] { override val value: Unit = () }

  case class Variable(name: String) extends Expression
  case class ColumnRef(name: String) extends Expression

  case class BinOp(op: String, lhs: Expression, rhs: Expression) extends Expression
  case class UnaryOp(op: String, v: Expression) extends Expression
  case class InOp(needle: Expression, haystack: List[Expression], notIn: Boolean) extends Expression

  case class FunctionCall(name: String, args: List[Expression]) extends Expression

  case class ArrayAccess(base: Expression, position: Expression) extends Expression

  case class NamedNode(name: String, value: Expression) extends Expression


  case class WhenThen(when: Expression, thenn: Expression)
  case class IfThenElse(iffs: List[WhenThen], elze: Option[Expression]) extends Expression

  case class Error(msg: String) extends Expression
}
