package data

import play.api.libs.json.{JsString, JsValue, Json, OWrites, Writes}
import qgis.expressions.QgsCompiler

import scala.language.implicitConversions

object Styles {
  private var FuncNumber = 0

  sealed trait Expression
  case class SimpleExpression(value: String) extends Expression
  case class CodeExpression(funcName: String, compiledCode: String) extends Expression

  object Expression {
    def fromCode(code: String): Expression = {
      val fname = f"lyr_func_$FuncNumber"
      FuncNumber = FuncNumber + 1
      QgsCompiler(fname, code) match {
        case Some(jsCode) => CodeExpression(fname, jsCode)
        case None => SimpleExpression("Compile error")
      }
    }
  }

  implicit def stringToExpr(s: String): Expression = SimpleExpression(s)

  implicit val expressionWriter: Writes[Expression] = (o: Expression) => {
    val (value, typ: String) = o match {
      case ex: SimpleExpression => (JsString(ex.value), "simple")
      case ex: CodeExpression => (JsString(ex.funcName), "code")
    }

    Json.obj("value" -> value, "exprType" -> JsString(typ))
  }

  sealed trait Style {
    val styleType: String;
  }

  case class ColorFillStyle(color: Expression) extends Style {
    override val styleType: String = "color";
  }

  case class LineFillStyle(color: String, width: Int) extends Style {
    override val styleType: String = "line";
  }

  case class AttributeBasedStyle(attribute: String, `default`: Style, mapping: Map[String, Style]) extends Style {
    override val styleType: String = "attributeBased";
  }

  case class LabelTextStyle(content: Expression = "Missing text", offsetX: Int = 0, offsetY: Int = 0,
                            font: Expression = "10px \\'Open Sans\\', sans-serif", color: Expression = "#323232",
                            overflow: Boolean = false) extends Style {
    override val styleType: String = "label"
  }

  case class UnionStyle(styles: List[Style]) extends Style {
    override val styleType: String = "union"
  }

  implicit val styleWriter: Writes[Style] = (o: Style) => {
    val jsObject = o match {
      case c: ColorFillStyle => ColorFillStyleWriter.writes(c)
      case a: AttributeBasedStyle => AttributeBasedStyleWriter.writes(a)
      case a: LineFillStyle => LineFillStyleWriter.writes(a)
      case a: LabelTextStyle => LabelTextStyleWriter.writes(a)
      case a: UnionStyle => UnionStyleWriter.writes(a)
    }
    jsObject + ("styleType" -> JsString(o.styleType))
  }

  implicit val ColorFillStyleWriter: OWrites[ColorFillStyle] = Json.writes[ColorFillStyle]
  implicit val LineFillStyleWriter: OWrites[LineFillStyle] = Json.writes[LineFillStyle]
  implicit val LabelTextStyleWriter: OWrites[LabelTextStyle] = Json.writes[LabelTextStyle]
  implicit val UnionStyleWriter: OWrites[UnionStyle] = Json.writes[UnionStyle]
  implicit val AttributeBasedStyleWriter: OWrites[AttributeBasedStyle] = Json.writes[AttributeBasedStyle]

}
