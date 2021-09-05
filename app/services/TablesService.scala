package services

import data.LayerDef.TableDef

import scala.concurrent.Future


trait TablesService {
  def getTables: Future[Map[String, TableDef]]

  def getTable(table: String): Future[Option[TableDef]]
}
