package utils

import data.UserSession
import pdi.jwt.JwtSession
import pdi.jwt.JwtSession.RichRequestHeader
import play.api.Configuration
import play.api.mvc.RequestHeader

import java.time.Clock

object AuthenticationHeaders {

  implicit class UserRequestHeader(request: RequestHeader)(implicit conf: Configuration, clock: Clock) {
    private def session(cookiesAllowed: Boolean = false) = {
      val jwtHeader = request.jwtSession

      if (cookiesAllowed && jwtHeader.isEmpty() && request.cookies.get("session").nonEmpty) {
        request.cookies.get("session")
          .map(_.value.trim)
          .map(JwtSession.deserialize)
          .orElse(Some(jwtHeader))
      } else Some(jwtHeader)
    }.filter(_.claim.isValid)

    def optUser: Option[UserSession] = session().flatMap(_.getAs[UserSession]("user"))
    def optUserWithCookies: Option[UserSession] = session(true).flatMap(_.getAs[UserSession]("user"))

    def user: UserSession = optUser.get

    def eventIdOpt: Option[Int] = session().flatMap(_.getAs[Int]("event"))

    def eventId: Int = eventIdOpt.getOrElse(0)
  }
}