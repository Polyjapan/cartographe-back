package data.static

import data.LayerDef.TableDef
import services.TablesService

import scala.collection.mutable
import scala.concurrent.Future

object Tables extends TablesService {
  val Tables: Map[String, TableDef] = {
    val map = mutable.Map[String, TableDef]()

    def register(td: TableDef) = map.put(td.tablePrefix, td)

    register(TableDef("stands", List("id_pj", "exposant", "type", "prix", "nb_tables", "nb_chaises", "nb_panneaux", "commentaires", "puissance_elec_requise", "link")))


    map.toMap
  }

  def getTables: Future[Map[String, TableDef]] = Future.successful(Tables)

  def getTable(table: String): Future[Option[TableDef]] = Future.successful(Tables.get(table))
}