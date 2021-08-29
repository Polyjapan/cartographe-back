package services

import anorm._
import data.LayerDef
import data.LayerDef.{Feature, LayerData, LayerGroup, MultiDimensionLayer}
import play.api.db.Database
import services.PostGisDatabaseHelper._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LayerReader @Inject()(db: Database)(implicit ec: ExecutionContext) {

  def readAllDimensionsFromGroups(groups: List[LayerGroup[MultiDimensionLayer]]): Future[List[(String, List[(String, List[Feature])])]] = {
    Future.sequence(groups.map(grp => readAllDimensionsFromGroup(grp).map(layers => grp.name -> layers)))
  }

  def readAllDimensionsFromGroup(group: LayerGroup[MultiDimensionLayer]): Future[List[(String, List[Feature])]] = {
    Future.sequence(group.layers.map(layer => readAllDimensionsFromLayer(layer).map(features => layer.prettyName.getOrElse(layer.tablePrefix) -> features))) // .map(_.toMap)
  }

  def readAllDimensionsFromLayer(implicit layer: LayerDef.MultiDimensionLayer): Future[List[Feature]] =
    Future.sequence {
      for {dim <- layer.dimensions} yield readLayer(layer, dim)
    }.map(_.flatten)

  def readLayer(layer: LayerDef.MultiDimensionLayer, dimension: String): Future[List[Feature]] = Future {
    db.withConnection(implicit conn => {

      SQL("SELECT * FROM " + layer.tblName(dimension)).as(parser(dimension)(layer).*)
    })
  }

  private def parser(dim: String)(implicit layer: LayerDef.MultiDimensionLayer): RowParser[Feature] = (row: Row) => {

    Try {
      val attributes = {
        val attrBase = ("id" :: layer.attributes).map(e => e -> row[String](e)(readAllAsString)).toMap

        attrBase + ("uid" -> s"${layer.dbGroup}.${layer.tablePrefix}_${dim}#${attrBase("id")}")
      }
      val geom = row(layer.geometryColumn)(geometryParser)

      Feature(geom.getGeometry, layer.tablePrefix, dim, attributes, attributes("id").toInt)
    } match {
      case scala.util.Success(value) => Success(value)
      case scala.util.Failure(exception) => Error(SqlRequestError(exception))
    }
  }

  def readGroups(groups: List[LayerGroup[MultiDimensionLayer]], dimension: String): Future[List[LayerGroup[LayerData]]] =
    Future.sequence(groups.map(group => readGroup(group, dimension)))

  def readGroup(group: LayerGroup[MultiDimensionLayer], dimension: String): Future[LayerGroup[LayerData]] =
    Future.sequence(group.layers.map(lyr => readLayerToLayerData(lyr, dimension))).map(lst => group.copy[LayerData](layers = lst))

  implicit def readAllAsString: Column[String] =
    Column { (value, meta) =>
      value match {
        case el: Int => Right(s"$el") // Provided-default case
        case el: String => Right(el)
        case el if el == null => Right("null")
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to String for column ${meta.column}"))
      }
    }

  def readLayerToLayerData(layer: LayerDef.MultiDimensionLayer, dimension: String): Future[LayerData] =
    readLayer(layer, dimension).map(data => LayerData(layer.prettyName.getOrElse(layer.tablePrefix.capitalize), layer.tblName(dimension), data, layer.style))
}
