package qgis.expressions

import qgis.expressions
import qgis.expressions.Tokens._

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Positional, Reader}

object Parser extends Parsers {
  override type Elem = Tokens.Token


  def variable: Parser[Tree.Variable] = positioned(accept("variable", { case Variable(name) => Tree.Variable(name) }))

  // Special Columns are just functions with no argument!
  def specialColumn: Parser[Tree.FunctionCall] = positioned(accept("special column", { case SpecialColumn(name) =>
    val func = Functions(name)
    if (func.isEmpty) println(s"WARN: unknown function ${name}")
    Tree.FunctionCall(name, List())
  }))
  def unquotedIdentifier: Parser[String] = accept("identifier", { case Identifier(name) => name })

  def literals: Parser[Tree.Literal[_]] = positioned {
    accept("literal", {
      case BooleanLiteral(b) => Tree.BooleanLiteral(b)
      case StringLiteral(s) => Tree.StringLiteral(s)
      case IntLiteral(i) => Tree.IntLiteral(i)
      case FloatLiteral(i) => Tree.FloatLiteral(i)
      case NullLiteral() => Tree.NullLiteral()
    })
  }

  def condition: Parser[Tree.Expression] = positioned {
    def whenThen = (When() ~> expr) ~ (Then() ~> expr) ^^ { case w ~ t => Tree.WhenThen(w, t) }

    def elze = Else() ~> expr

    (Case() ~> whenThen.+) ~ elze.? <~ End() ^^ {
      case iff ~ elzeOpt => Tree.IfThenElse(iff, elzeOpt)
    }
  }

  def valueList: Parser[List[Tree.Expression]] = {
    def namedNode: Parser[Tree.Expression => Tree.Expression] = accept("named node", {
      case NamedNode(n) => (a: Tree.Expression) => Tree.NamedNode(n, a)
    })

    def valueOrNamedNode: Parser[Tree.Expression] = positioned {
      expr | (namedNode ~ expr) ^^ { case fn ~ v => fn(v) }
    }

    repsep(valueOrNamedNode, Comma())
  }

  def functionCallOrColumnName: Parser[Tree.Expression] = positioned {
    def quotedIdentifier: Parser[Tree.ColumnRef] =
      accept("quoted identifier", { case QuotedIdentifier(name) => Tree.ColumnRef(name) })

    def unquotedIdentifierOrFunction = (unquotedIdentifier ~ (LPar() ~>! valueList <~ RPar()).?) ^^ ( {
      case id ~ Some(args) =>
        val func = Functions(id)
        if (func.isEmpty) {
          println(s"WARN: unknown function ${id}")
        }
        Tree.FunctionCall(id.toLowerCase, args)
      case id ~ None => Tree.ColumnRef(id)
    })

    quotedIdentifier | unquotedIdentifierOrFunction
  }

  def valueInParenthesis: Parser[Tree.Expression] = LPar() ~>! expr <~! RPar()

  def valuePrefixedWithUnaryOp: Parser[Tree.UnaryOp] = positioned {
    val prefixes = Set("NOT", "+", "-")

    def prefix: Parser[Tree.Expression => Tree.UnaryOp] = accept("prefix", {
      case Keyword(kw) if prefixes(kw) => (a: Tree.Expression) => Tree.UnaryOp(kw, a)
    })

    prefix ~ expr ^^ { case fn ~ v => fn(v) }
  }

  def baseValue: Parser[Tree.Expression] = positioned {
    def baseBaseValue: Parser[Tree.Expression] = (
      literals
        | variable
        | specialColumn
        | valueInParenthesis
        | valuePrefixedWithUnaryOp
        | functionCallOrColumnName
        | condition
      )

    def arraySuffix: Parser[Tree.Expression] = LBrack() ~> expr <~ RBrack()

    baseBaseValue ~ arraySuffix.? ^^ {
      case v ~ None => v
      case v ~ Some(e) => Tree.ArrayAccess(v, e)
    }
  }

  def expr: Parser[Tree.Expression] = positioned {
    def iterPriority(opPriority: List[Set[String]]): Parser[Tree.Expression] = opPriority match {
      case head :: tail =>
        val lowerPriorityValue = {
          if (tail.isEmpty) baseValue
          else iterPriority(tail)
        }

        def currentPriorityParse: Parser[Tree.Expression] = chainl1(
          lowerPriorityValue, currentPriorityParse, accept("operator", {
            case Keyword(kw) if head(kw) => (a: Tree.Expression, b: Tree.Expression) => Tree.BinOp(kw, a, b)
          }))

        currentPriorityParse
    }

    val operatorsByPriority = List(
      Set("AND", "OR"),
      Set("<", "<=", ">=", ">", "<>", "!=", "="),
      Set("IS", "IS NOT", "LIKE", "NOT LIKE", "ILIKE", "NOT ILIKE", "~"),
      Set("+", "-"),
      Set("/", "%", "//", "^", "||")
    )

    val topLevelOps = iterPriority(operatorsByPriority)

    def inOps = {
      def parseInOps =
        (Keyword("IN") ^^^ false | Keyword("NOT IN") ^^^ true) ~ (LPar() ~> valueList <~ RPar())

      (topLevelOps ~ parseInOps.?) ^^ {
        case a ~ None => a
        case a ~ Some(b ~ vList) => Tree.InOp(a, vList, b)
      }
    }

    inOps
  }

  def apply(tokens: Seq[Token]): Either[CompileError, Tree.Expression] = compile(tokens, expr)

  private def compile[A <: Positional](tokens: Seq[Token], parser: Parser[A]): Either[CompileError, A] = {
    positioned(phrase(parser))(new TokenReader(tokens)) match {
      case NoSuccess(msg, next) => Left(CompileError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, _) => Right(result)
    }
  }

  class TokenReader(tokens: Seq[Token]) extends Reader[Token] {
    override def rest: Reader[Token] = new TokenReader(tokens.tail)

    override def pos: Position = if (atEnd) NoPosition else first.pos

    override def first: Token = tokens.head

    override def atEnd: Boolean = tokens.isEmpty
  }
}
