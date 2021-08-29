package services

import data.LayerDef.LayerData
import data.Styles.Style
import org.postgis.{Geometry, Point, Polygon}
import play.api.libs.json._

object GeoJson {
  case class GeoJsonFeature(`type`: String = "Feature", properties: Map[String, JsValue], geometry: Geometry, id: Int)

  case class GeoJsonLayer(`type`: String = "FeatureCollection",
                          title: String,
                          tableName: String,
                          features: List[GeoJsonFeature],
                          style: Option[Style],
                         `crs`: JsValue = Json.obj("type" -> "name", "properties" -> Json.obj("name" -> "EPSG:2056")))


  implicit val writeGeometryToGeoJson: Writes[Geometry] = {
    case poly: Polygon =>
      val points = (0 until poly.numPoints()) map (poly.getPoint)
      Json.obj("type" -> JsString("Polygon"), "coordinates" -> JsArray(List(JsArray(points.map(point => JsArray(List(JsNumber(point.x), JsNumber(point.y))))))))
    case point: Point => Json.obj("type" -> JsString("Point"), "x" -> JsNumber(point.x), "y" -> JsNumber(point.y), "z" -> JsNumber(point.z))
  }
  implicit val featureWrites: Writes[GeoJsonFeature] = Json.writes[GeoJsonFeature]
  implicit val layerWrites: Writes[GeoJsonLayer] = Json.writes[GeoJsonLayer]

  def layerToGeoJson(layer: LayerData): GeoJsonLayer = {
    GeoJsonLayer(title = layer.title, features = layer.features.map(
      ft => GeoJsonFeature(properties = ft.data.view.mapValues(s => JsString(s)).toMap, geometry = ft.geometry, id = ft.id)
    ), style = layer.style, tableName = layer.tableName)
  }

  def layerToJson(layer: LayerData): JsValue = Json.toJson(layerToGeoJson(layer))
}
