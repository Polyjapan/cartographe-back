package services

import data.LayerDef.LayerData
import data.Styles.Style
import org.postgis.{Geometry, LineString, MultiLineString, Point, Polygon}
import play.api.libs.json._

object GeoJson {

  def layerToJson(layer: LayerData): JsValue = Json.toJson(layerToGeoJson(layer))

  def layerToGeoJson(layer: LayerData): GeoJsonLayer = {
    GeoJsonLayer(title = layer.title, features = layer.features.map(
      ft => GeoJsonFeature(properties = ft.data.view.mapValues(s => JsString(s)).toMap, geometry = ft.geometry, id = ft.id)
    ), style = layer.style, tableName = layer.tableName)
  }


  implicit val writeGeometryToGeoJson: Writes[Geometry] = { geom =>
    def point2coords(point: Point) = JsArray(List(JsNumber(point.x), JsNumber(point.y)))

    def geomType(geometry: Geometry) = geometry match {
      case poly: Polygon => "Polygon"
      case point: Point => "Point"
      case ms: MultiLineString => "MultiLineString"
      case ls: LineString => "LineString"
    }

    def geoCoords(geometry: Geometry): JsArray = geometry match {
      case poly: Polygon =>
        val points = (0 until poly.numPoints()) map (poly.getPoint)
        JsArray(List(JsArray(points.map(point2coords))))
      case point: Point => point2coords(point)
      case ms: MultiLineString => JsArray(ms.getLines.map(geoCoords))
      case ls: LineString => JsArray(ls.getPoints.map(point2coords))
    }

    Json.obj("type" -> JsString(geomType(geom)), "coordinates" -> geoCoords(geom))
  }

  implicit val featureWrites: Writes[GeoJsonFeature] = Json.writes[GeoJsonFeature]
  implicit val layerWrites: Writes[GeoJsonLayer] = Json.writes[GeoJsonLayer]

  case class GeoJsonFeature(`type`: String = "Feature", properties: Map[String, JsValue], geometry: Geometry, id: Int)

  case class GeoJsonLayer(`type`: String = "FeatureCollection",
                          title: String,
                          tableName: String,
                          features: List[GeoJsonFeature],
                          style: Option[Style],
                          `crs`: JsValue = Json.obj("type" -> "name", "properties" -> Json.obj("name" -> "EPSG:2056")))
}
