package controllers

import data.LayerDef
import data.LayerDef._
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc._
import services.{GeoJson, LayerReader}

import javax.inject._
import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, layerReader: LayerReader)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action.async {
    /*db.withConnection(implicit c => {
      val res = SQL("SELECT geom FROM ji_2022.stands_1 LIMIT 1").as(get[PGgeometry]("geom").single)
      println(res)

      res.getGeometry match {
        case poly: Polygon =>
          println(poly.getFirstPoint)
      }
    })

    Ok(views.html.index("Your new application is ready."))*/
    layerReader.readAllDimensionsFromGroups(LayerDef.Layers).map(lst => Ok(Json.toJson(lst)))
  }

  def wmsForDimension(dimension: String) = Action {
    val data: Seq[LayerDef.LayerGroup[LayerDef.MultiDimensionWMSLayer]] = LayerDef.BaseLayers.map(grp => grp.copy(layers = grp.layers.map(lyr => lyr.forDimension(dimension))))
    Ok(Json.toJson(data))
  }

  def dataForDimension(dimension: String) = Action.async {
    layerReader.readGroups(LayerDef.Layers, dimension).map(groups => Ok {
      Json.toJson(groups.map(grp => Json.obj("name" -> JsString(grp.name), "layers" -> JsArray(grp.layers.map(GeoJson.layerToJson)))))
    })
  }
}
