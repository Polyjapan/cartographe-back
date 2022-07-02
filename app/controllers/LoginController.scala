package controllers

import akka.http.scaladsl.util.FastFuture.successful
import ch.japanimpact.auth.api.cas.CASService
import data.UserSession
import pdi.jwt.JwtSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.StaffsService

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class LoginController @Inject()(cc: ControllerComponents, cas: CASService, staffs: StaffsService)(implicit ec: ExecutionContext, clock: Clock, config: Configuration) extends AbstractController(cc) {

  def login(token: String) = Action.async {
    cas.proxyValidate(token, None) flatMap {
      case Left(err) =>
        Future successful BadRequest(Json.obj("error" -> err.errorType.toString, "message" -> err.message))
      case Right(data) =>
        val user =  UserSession(data)

        staffs.isStaff(data.user.toInt) map { isStaff =>
          if (isStaff) user.copy(groups = user.groups + "internal__staff")
          else user
        } map { user =>
          val session: JwtSession = JwtSession() + ("user", user)
          Ok(Json.toJson(Json.obj("session" -> session.serialize)))
        }

    }
  }

}
