package services

import data.LayerDef.{LayerGroup, MapDef, MultiDimensionLayer, MultiDimensionWMSLayer}

import scala.concurrent.Future


trait MapsService {
  /**
   * Gets all the available maps (must be filtered by groups)
   * @return
   */
  def getMaps: Future[List[MapDef]]

  /**
   * Gets the base layers of a given map (must be filtered by groups)
   * @param mapId
   * @return
   */
  def getBaseLayers(mapId: Int): Future[Option[(MapDef, List[LayerGroup[MultiDimensionWMSLayer]])]]

  /**
   * Gets the json layers of a given map (must be filtered by groups)
   * @param mapId
   * @return
   */
  def getJsonLayers(mapId: Int): Future[Option[(MapDef, List[LayerGroup[MultiDimensionLayer]])]]
}
