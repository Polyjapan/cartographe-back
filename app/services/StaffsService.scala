package services

import ch.japanimpact.api.events.EventsService
import ch.japanimpact.staff.api.StaffsApi
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StaffsService @Inject()(val staffs: StaffsApi, events: EventsService)(implicit val ec: ExecutionContext) extends Logging {

   /**
    * Checks if the given user is a staff at the current event
    * @param userId
    * @return
    */
   def isStaff(userId: Int) = {
      events.getCurrentEvent() flatMap {
         case Right(value) => staffs.isStaff(value.event.id.get, userId)
         case Left(left) => Future successful Left(left)
      } map {
         case Right(v) => v
         case Left(ex) => logger.error("Staffs API returned following error: " + ex) ; false
      }
   }
}
