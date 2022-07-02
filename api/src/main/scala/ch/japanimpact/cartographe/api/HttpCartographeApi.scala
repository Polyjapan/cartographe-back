package ch.japanimpact.cartographe.api

import akka.Done
import ch.japanimpact.api.APIError
import ch.japanimpact.auth.api.{AbstractHttpApi, GenericHttpApi, GroupsApi}
import ch.japanimpact.auth.api.apitokens.APITokensService
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsArray, JsObject}
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HttpCartographeApi @Inject()(implicit ec: ExecutionContext, ws: WSClient, config: Configuration, tokens: APITokensService, cache: AsyncCacheApi)
  extends GenericHttpApi("cartographe", Set("plans/*"), Set("cartographe")) with CartographeApi {

  class HttpTableApi(val table: String) extends TableApi {
    override def updateEntity(dimension: String, id: Int, content: JsObject): Future[APIResult[Done]] =
      withTokenToDone(s"/data/$table/$dimension/$id")(_.patch(content))

    override def getEntities: Future[APIResult[List[JsObject]]] = withToken(s"/data/$table")(_.get())(_.as[JsArray].value.map(_.as[JsObject]).toList)
  }

  override def table(name: String): TableApi = new HttpTableApi(name)
}
