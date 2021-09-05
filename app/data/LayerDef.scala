package data

import data.Styles.{AttributeBasedStyle, ColorFillStyle, LabelTextStyle, LabelTextStyleWriter, UnionStyle}
import org.postgis.{Geometry, Point, Polygon}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OWrites, Writes}
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.MultipartUtils

object LayerDef {
  sealed trait Layer

  case class TableDef(tablePrefix: String, columns: List[String], dimensions: List[String] = List("0", "1", "2"),
                                          geometryColumn: String = "geom", dbGroup: String = "ji_2022") {
    def tblName(dimension: String) = s"${dbGroup}.${tablePrefix}_$dimension"

  }


  case class MultiDimensionLayer(table: TableDef, prettyName: Option[String], dimensionName: String,
                                 style: Option[Styles.Style]) extends Layer

  object MultiDimensionLayer {
    def apply(tablePrefix: String, attributes: List[String], prettyName: Option[String] = None,
              dimensions: List[String] = List("0", "1", "2"), dimensionName: String = "Étage",
              geometryColumn: String = "geom",
              dbGroup: String = "ji_2022", style: Option[Styles.Style] = None): MultiDimensionLayer = {
      val table = TableDef(tablePrefix, attributes, dimensions, geometryColumn, dbGroup)

      MultiDimensionLayer(table, prettyName, dimensionName, style)
    }
  }

  case class MultiDimensionWMSLayer(params: Map[String, String], title: String, defaultVisibility: Boolean = true,
                                    url: String = "https://geoportail.epfl.ch/prod/wsgi/mapserv_proxy?VERSION=1.3.0&floor={dimension}",
                                    dimensions: List[String] = List("0", "1", "2"),
                                    dimensionName: String = "Étage",
                                    kind: String = "WMS",
                                    zIndex: Int = 0) extends Layer {
    private val fullParams: Map[String, String] = Map("TILED" -> "true", "VERSION" -> "1.3.0") ++ params

    def forDimension(dim: String): MultiDimensionWMSLayer =
      copy(params = fullParams.view.mapValues(v => v.replaceAll("\\{dimension}", dim)).toMap, url = url.replaceAll("\\{dimension}", dim), dimensions = List(dim))
  }

  case class LayerGroup[T <: Layer](name: String, layers: List[T])

  case class Feature(geometry: Geometry, layer: String, dimension: String, data: Map[String, String], id: Int)

  case class LayerData(title: String, tableName: String, features: List[Feature], style: Option[Styles.Style] = None) extends Layer

  implicit val geomWrites: Writes[Geometry] = new Writes[Geometry] {
    override def writes(o: Geometry): JsValue = o match {
      case poly: Polygon =>
        val points = (0 until poly.numPoints()) map (poly.getPoint)
        Json.obj("type" -> JsString("polygon"), "points" -> JsArray(points.map(writes)))
      case point: Point => Json.obj("type" -> JsString("point"), "x" -> JsNumber(point.x), "y" -> JsNumber(point.y), "z" -> JsNumber(point.z))
    }
  }
  implicit val featureWrite: OWrites[Feature] = Json.writes[Feature]
  implicit val layerDataWrite: OWrites[LayerData] = Json.writes[LayerData]
  implicit val multiDimensionWMSLayerWrite: OWrites[MultiDimensionWMSLayer] = Json.writes[MultiDimensionWMSLayer]
  implicit val layerGroupWrite: OWrites[LayerGroup[MultiDimensionWMSLayer]] = Json.writes[LayerGroup[MultiDimensionWMSLayer]]
  implicit val layerGroupDataWrite: OWrites[LayerGroup[LayerData]] = Json.writes[LayerGroup[LayerData]]

  case class MapDef(mapId: Int, title: String, description: String, requiredGroups: Set[String]) {
    def readableBy(user: Option[UserSession]): Boolean = {
      val userGroups = user.map(_.groups).getOrElse(Set())
      requiredGroups.isEmpty || requiredGroups.exists(group => userGroups.contains(group))
    }
  }
  implicit val mapDefWrites: OWrites[MapDef] = Json.writes[MapDef]

}
