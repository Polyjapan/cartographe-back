package data

import ch.japanimpact.auth.api.cas.CASServiceResponse
import play.api.libs.json.{Format, Json}


case class UserSession(userId: Int, email: String, groups: Set[String], firstName: String)

object UserSession {
  def apply(rep: CASServiceResponse): UserSession =
    UserSession(rep.user.toInt, rep.email.get, rep.groups, rep.firstname.get)

  implicit val format: Format[UserSession] = Json.format[UserSession]
}
