package data

import play.api.libs.json.{JsString, JsValue, Json, OWrites, Writes}

object Styles {
  sealed trait Style {
    val styleType: String;
  }

  case class ColorFillStyle(color: String) extends Style {
    override val styleType: String = "color";
  }

  case class LineFillStyle(color: String, width: Int) extends Style {
    override val styleType: String = "line";
  }

  case class AttributeBasedStyle(attribute: String, `default`: Style, mapping: Map[String, Style]) extends Style {
    override val styleType: String = "attributeBased";
  }

  case class LabelTextStyle(content: String, offsetX: Int = 0, offsetY: Int = 0,
                            font: String = "12.5px \\'Open Sans\\', sans-serif", color: String = "#323232", overflow: Boolean = false) extends Style {
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
