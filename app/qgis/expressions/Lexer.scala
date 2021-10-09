package qgis.expressions

import scala.util.parsing.combinator.RegexParsers
import Tokens._

// https://github.com/qgis/QGIS/blob/23e232317667f9ccb50f53638f0c01627469664d/src/core/qgsexpressionlexer.ll

object Lexer extends RegexParsers {
  override def skipWhitespace = false

  def whitespace: Parser[Whitespace] = {
    def comment: Parser[Char] = ("[^*]*".r ~ '*' ~ '/' | "[^*]*".r ~ '*' ~ comment) ^^^ ' '
    (
      """\s+""".r
        | '/' ~> '*' ~> comment
        | '-' ~ '-' ~ "[^\n\r]*".r ~ "[\r\n]?".r
        | '/' ~> '*' ~> failure("unclosed comment")
    ) ^^^ Whitespace()
  }

  def caseInsensitive(str: String): Parser[String] = ("""(?i)\Q""" + str + """\E""").r

  def identifier: Parser[Token] = positioned {
    def unquoted = "[a-zA-Z_][a-zA-Z0-9_]*".r ^? { case str if !reserved(str.toUpperCase) => Identifier(str) }
    def variable = '@' ~> "[a-zA-Z0-9_]*".r ^^ { str => Variable(str) }
    def specialCol = '$' ~> "[a-zA-Z0-9_]*".r ^^ { str => SpecialColumn(str) }

    def quotedChar = "\\\"" | "[^\\\"]".r

    def quoted = '"' ~>! quotedChar.* <~ '"' ^^ { str => QuotedIdentifier(str.mkString) }

    def unquotedOrNamedNode = {
      unquoted ~ (whitespace.? ~ ":=").? ^^ {
        case a ~ None => a
        case a ~ Some(_) => NamedNode(a.name)
      }
    }

    quoted | unquotedOrNamedNode | variable | specialCol
  }

  def stringLiteral: Parser[StringLiteral] = positioned {
    "'" ~> repsep("[^'\n]*".r, "\\'") <~ "'" ^^ { lst => StringLiteral(lst.mkString("'")) }
  }

  def intLiteral: Parser[IntLiteral] = positioned {
    "[0-9]+".r ^^ { str => IntLiteral(str.toInt) }
  }

  def floatLiteral: Parser[FloatLiteral] = positioned {
    "[0-9]*(\\.[0-9]+([eE][-+]?[0-9]+)?|[eE][-+]?[0-9]+)".r ^^ { str => FloatLiteral(str.toFloat) }
  }

  def booleanLiteral: Parser[BooleanLiteral] = positioned {
    caseInsensitive("TRUE") ^^^ BooleanLiteral(true) | caseInsensitive("FALSE") ^^^ BooleanLiteral(false)
  }

  def nullLiteral: Lexer.Parser[NullLiteral] = positioned(caseInsensitive("NULL") ^^^ NullLiteral())

  val reserved = Set("NOT", "AND", "OR", "LIKE", "ILIKE", "IS", "NOT", "IN", "TRUE", "FALSE", "NULL",
    "CASE", "WHEN", "THEN", "ELSE", "END")

  def keywords: Lexer.Parser[Keyword] = positioned {
    ( caseInsensitive("NOT") ~ (whitespace ~ (
        caseInsensitive("LIKE") | caseInsensitive("ILIKE") | caseInsensitive("IN")
      )).? ^^ {
      case a ~ None => a
      case a ~ Some(b) => a + " " + b
    }
      | caseInsensitive("AND")
      | caseInsensitive("OR")
      | "=" | "!=" | "<=" | ">=" | "<>" | "<" | ">" | "~"
      | caseInsensitive("LIKE")
      | caseInsensitive("ILIKE")
      | caseInsensitive("IS") ~ (whiteSpace ~ caseInsensitive("NOT")).? ^^ {
      case a ~ None => a
      case a ~ Some(b) => "IS NOT"
    }
      // not sure these are really needed...
      // cont
      | "||" | "+" | "-" | "*" | "/" | "//" | "%" | "^"
      | caseInsensitive("IN")
      ) ^^ { op => Keyword(op.toUpperCase) }
  }

  def conditional: Parser[Token with ConditionalToken] = positioned {
    ( caseInsensitive("CASE") ^^^ Case()
      | caseInsensitive("WHEN") ^^^ When()
      | caseInsensitive("THEN") ^^^ Then()
      | caseInsensitive("ELSE") ^^^ Else()
      | caseInsensitive("END") ^^^ End()
      )
  }

  def symbols = positioned(
    "(" ^^^ LPar()
    | ")" ^^^ RPar()
    | "," ^^^ Comma()
    | "[" ^^^ LBrack()
    | "]" ^^^ RBrack()
  )

  def tokens: Parser[List[Token]] = {
    def kw = keywords | conditional | nullLiteral | booleanLiteral
    def otherLit = intLiteral | floatLiteral | stringLiteral

    phrase(
      rep((whitespace |
        (kw ||| identifier) | symbols | otherLit
      ))
    ) ^^ { lst => lst.filterNot(tk => tk == Whitespace()) } // drop whitespaces
  }

  def apply(code: String): Either[CompileError, List[Token]] = {
    println("Lexing")
    println(code)
    parse(tokens, code) match {
      case NoSuccess(msg, next) => Left(CompileError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, _) => Right(result)
    }
  }
}
