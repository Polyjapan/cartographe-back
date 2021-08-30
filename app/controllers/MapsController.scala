package controllers

import data.LayerDef
import data.LayerDef._
import play.api.Configuration
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc._
import services.{GeoJson, LayerReader, MapsService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import utils.AuthenticationHeaders._

import java.time.Clock

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MapsController @Inject()(cc: ControllerComponents, layerReader: LayerReader, mapsService: MapsService)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {

  def listMaps: Action[AnyContent] = Action.async { req =>
    mapsService.getMaps.map(lst => lst.filter(map => map.readableBy(req.optUser))).map(lst => Ok(Json.toJson(lst)))
  }

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = TODO
    /*db.withConnection(implicit c => {
      val res = SQL("SELECT geom FROM ji_2022.stands_1 LIMIT 1").as(get[PGgeometry]("geom").single)
      println(res)

      res.getGeometry match {
        case poly: Polygon =>
          println(poly.getFirstPoint)
      }
    })

    Ok(views.html.index("Your new application is ready."))*/
    // layerReader.readAllDimensionsFromGroups(LayerDef.Layers).map(lst => Ok(Json.toJson(lst)))


  def wmsForDimension(map: Int, dimension: String) = Action.async { req =>
    this.mapsService.getBaseLayers(map)
      .map(_.filter(_._1.readableBy(req.optUser)))
      .map {
        case Some((_, layers)) =>
          Ok(Json.toJson(layers.map(grp => grp.copy(layers = grp.layers.map(lyr => lyr.forDimension(dimension))))))
        case None => NotFound
      }
  }

  def dataForDimension(map: Int, dimension: String) = Action.async { req =>
    this.mapsService.getJsonLayers(map)
      .map(_.filter(_._1.readableBy(req.optUser)))
      .flatMap {
        case Some((_, layers)) =>
          layerReader.readGroups(layers, dimension).map(groups => Ok {
            Json.toJson(groups.map(grp => Json.obj("name" -> JsString(grp.name), "layers" -> JsArray(grp.layers.map(GeoJson.layerToJson)))))
          })
        case None => Future successful NotFound
      }
  }
}
