package ch.japanimpact.cartographe.api

import akka.Done
import ch.japanimpact.api.APIError
import ch.japanimpact.staff.api.StaffsApi.Staff
import com.google.inject.ImplementedBy
import play.api.libs.json.JsObject

import scala.concurrent.Future

@ImplementedBy(classOf[HttpCartographeApi])
trait CartographeApi {
  type APIResult[T] = Either[APIError, T]

  /**
   * Get a geographic table to operate on
   * @param name
   * @return
   */
  def table(name: String): TableApi

  trait TableApi {
    def updateEntity(dimension: String, id: Int, content: JsObject): Future[APIResult[Done]]

    def getEntities: Future[APIResult[List[JsObject]]]
  }

}


