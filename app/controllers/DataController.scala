package controllers

import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import play.api.{Configuration, Logger}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.{DatabaseService, TablesService}

import java.time.Clock
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DataController @Inject()(cc: ControllerComponents, dbService: DatabaseService, tblService: TablesService, auth: AuthorizationActions)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {
  // TODO: take event as parameter!
  private val LOG = Logger("DataController")

  def updateEntity(table: String, dimension: String, id: Int) = auth().async(parse.json[JsObject]) { implicit req: auth.AuthorizedRequest[JsObject] =>
    tblService.getTable(table).map {
      case Some(tbl) if tbl.dimensions.contains(dimension) =>
        val table = tbl.tblName(dimension)
        val updates = req.body.fields
          .filter(pair => tbl.columns.contains(pair._1)) // only keep declared columns
          .filter(pair => req.principal.hasScope("plans/write/" + table + "/" + pair._1)) // check if user has right to write

        LOG.info(f"Updating entity $table $id with $updates")

        if (updates.nonEmpty)
          dbService.update(table, id, updates.toSeq)


        Ok
      case Some(_) => NotFound("DIMENSION")
      case None => NotFound("TABLE")
    }
  }

  def getEntities(table: String) = auth().async { req =>
    tblService.getTable(table).flatMap {
      case Some(layer) =>
        dbService.readAllDimensionsFromLayer(layer)
          .map(features =>
            features.map(feature =>
              feature.data.filter(pair => {
                val perm = "plans/read/" + table + "/" + pair._1

                req.principal.hasScope(perm)
              })
            ))
          .map(filtered => Ok(Json.toJson(filtered)))
      case _ => Future(NotFound)
    }
  }


}
