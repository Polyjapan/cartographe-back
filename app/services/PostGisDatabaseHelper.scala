package services

import anorm.{Column, TypeDoesNotMatch}
import org.postgis.PGgeometry

import scala.util.Try

object PostGisDatabaseHelper {
  implicit def geometryParser: Column[PGgeometry] =
    Column.nonNull { (value, meta) =>
      value match {
        case el: PGgeometry => Right(el) // Provided-default case
        case el: String => Try {
          new PGgeometry(el)
        }.toEither.left.map(er => TypeDoesNotMatch(er.toString))
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to PGgeometry for column ${meta.column}"))
      }
    }
}
