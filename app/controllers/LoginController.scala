package controllers

import ch.japanimpact.auth.api.UsersApi
import ch.japanimpact.auth.api.cas.CASService
import data.UserSession
import pdi.jwt.JwtSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.DatabaseService

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext


class LoginController @Inject()(cc: ControllerComponents, cas: CASService)(implicit ec: ExecutionContext, clock: Clock, config: Configuration) extends AbstractController(cc) {

  def login(token: String) = Action.async {
    cas.proxyValidate(token, None) map {
      case Left(err) =>
        BadRequest(Json.obj("error" -> err.errorType.toString, "message" -> err.message))
      case Right(data) =>
        val session: JwtSession = JwtSession() + ("user", UserSession(data))

        Ok(Json.toJson(Json.obj("session" -> session.serialize)))
    }
  }

}
