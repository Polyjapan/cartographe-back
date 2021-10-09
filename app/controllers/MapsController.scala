package controllers

import data.LayerDef
import data.LayerDef._
import data.Styles.{AttributeBasedStyle, AttributeBasedStyleWriter, CodeExpression, ColorFillStyle, ColorFillStyleWriter, Expression, LabelTextStyle, LabelTextStyleWriter, LineFillStyle, LineFillStyleWriter, Style, UnionStyle, UnionStyleWriter}
import play.api.Configuration
import play.api.http.MimeTypes
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc._
import services.{DatabaseService, GeoJson, MapsService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import utils.AuthenticationHeaders._

import java.time.Clock

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MapsController @Inject()(cc: ControllerComponents, layerReader: DatabaseService, mapsService: MapsService)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {

  def listMaps: Action[AnyContent] = Action.async { req =>
    mapsService.getMaps.map(lst => lst.filter(map => map.readableBy(req.optUser))).map(lst => Ok(Json.toJson(lst)))
  }

  /*
  // This is a version with access control.
  // I don't think the formatting rules themselves should be secret, and so I think protecting them is not worth
  // the loss in performance (non cachable)
  def getExpressions: Action[AnyContent] = Action.async { req =>
    def extractExpressions(style: Style): List[Expression] = style match {
      case c: ColorFillStyle => List(c.color)
      case a: AttributeBasedStyle => a.mapping.values.flatMap(extractExpressions).toList
      case a: LineFillStyle => List()
      case a: LabelTextStyle => List(a.color, a.content, a.font)
      case a: UnionStyle => a.styles.flatMap(extractExpressions)
    }

    mapsService.getMaps
      .flatMap { maps =>
        Future.sequence(maps.map(map => this.mapsService.getJsonLayers(map.mapId).map(layers => (map, layers))))
      }
      .map { lst =>
        lst
          .filter(map => map._1.readableBy(req.optUserWithCookies))
          .flatMap {
            case (_, Some((_, layers))) =>
              val functions = layers
                .flatMap(group => group.layers)
                .flatMap(layer => layer.style)
                .flatMap(extractExpressions)
                .filter {
                  case _: CodeExpression => true
                  case _ => false
                }
                .toSet

              functions.map {
                case c: CodeExpression => c.compiledCode
              }
            case _ => List()
          }
      }
      .map(lst => Ok(lst.mkString("\n\n")).as(MimeTypes.JAVASCRIPT).withHeaders("Cache-Control" -> "no-store"))
  }
   */

  def getExpressions: Action[AnyContent] = Action.async { req =>
    def extractExpressions(style: Style): List[Expression] = style match {
      case c: ColorFillStyle => List(c.color)
      case a: AttributeBasedStyle => a.mapping.values.flatMap(extractExpressions).toList
      case a: LineFillStyle => List()
      case a: LabelTextStyle => List(a.color, a.content, a.font)
      case a: UnionStyle => a.styles.flatMap(extractExpressions)
    }

    mapsService.getMaps
      .flatMap { maps =>
        Future.sequence(maps.map(map => this.mapsService.getJsonLayers(map.mapId).map(layers => (map, layers))))
      }
      .map { lst =>
        lst
          .flatMap {
            case (_, Some((_, layers))) =>
              val functions = layers
                .flatMap(group => group.layers)
                .flatMap(layer => layer.style)
                .flatMap(extractExpressions)
                .filter {
                  case _: CodeExpression => true
                  case _ => false
                }
                .toSet

              functions.map {
                case c: CodeExpression => c.compiledCode
              }
            case _ => List()
          }
      }
      .map(lst => Ok(lst.mkString("\n\n"))
        .as(MimeTypes.JAVASCRIPT)
        .withHeaders("Cache-Control" -> "public, max-age=86400"))
  }

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
