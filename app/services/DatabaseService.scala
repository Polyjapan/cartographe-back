package services

import anorm._
import data.LayerDef
import data.LayerDef.{Feature, LayerData, LayerGroup, MultiDimensionLayer}
import play.api.db.Database
import play.api.libs.json.{JsBoolean, JsNumber, JsString, JsValue}
import services.PostGisDatabaseHelper._
import scala.collection.immutable.Seq

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DatabaseService @Inject()(db: Database)(implicit ec: ExecutionContext) {

  def update(table: String, id: Int, pairs: Seq[(String, JsValue)]) = Future {
    def unpackParameter(name: String, jsValue: JsValue): NamedParameter = jsValue match {
      case JsNumber(value) => NamedParameter(name, value)
      case JsString(value) => NamedParameter(name, value)
      case JsBoolean(bool) => NamedParameter(name, bool)
    }


    db.withConnection(implicit conn => {
      val req = pairs.map(_._1).map(t => s"$t = {$t}").mkString(", ")
      val params: Seq[NamedParameter] = pairs.map(pair => unpackParameter(pair._1, pair._2)) :+ NamedParameter("int.id", id)


      SQL("UPDATE " + table + " SET " + req + " WHERE id = {int.id}")
        .on(params :_*)
        .executeUpdate()
    })
  }

  def readAllDimensionsFromGroups(groups: List[LayerGroup[MultiDimensionLayer]]): Future[List[(String, List[(String, List[Feature])])]] = {
    Future.sequence(groups.map(grp => readAllDimensionsFromGroup(grp).map(layers => grp.name -> layers)))
  }

  def readAllDimensionsFromGroup(group: LayerGroup[MultiDimensionLayer]): Future[List[(String, List[Feature])]] = {
    Future.sequence(group.layers.map(layer => readAllDimensionsFromLayer(layer.table).map(features => layer.prettyName.getOrElse(layer.table.tablePrefix) -> features))) // .map(_.toMap)
  }

  def readAllDimensionsFromLayer(implicit layer: LayerDef.TableDef): Future[List[Feature]] =
    Future.sequence {
      for {dim <- layer.dimensions} yield readLayer(layer, dim)
    }.map(_.flatten)

  def readLayer(layer: LayerDef.TableDef, dimension: String): Future[List[Feature]] = Future {
    db.withConnection(implicit conn => {

      SQL("SELECT * FROM " + layer.tblName(dimension)).as(parser(dimension)(layer).*)
    })
  }

  private def parser(dim: String)(implicit layer: LayerDef.TableDef): RowParser[Feature] = (row: Row) => {

    Try {
      val attributes = {
        val attrBase = ("id" :: layer.columns).map(e => e -> row[String](e)(readAllAsString)).toMap

        attrBase + ("uid" -> s"${layer.dbGroup}.${layer.tablePrefix}_${dim}#${attrBase("id")}") + ("dimension" -> dim)
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
    readLayer(layer.table, dimension).map(data => LayerData(layer.prettyName.getOrElse(layer.table.tablePrefix.capitalize), layer.table.tblName(dimension), data, layer.style))
}
