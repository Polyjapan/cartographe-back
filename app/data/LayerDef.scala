package data

import data.Styles.{AttributeBasedStyle, ColorFillStyle, LabelTextStyle, LabelTextStyleWriter, UnionStyle}
import org.postgis.{Geometry, Point, Polygon}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OWrites, Writes}
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.MultipartUtils

object LayerDef {
  sealed trait Layer

  case class MultiDimensionLayer(tablePrefix: String, attributes: List[String], prettyName: Option[String] = None,
                                 dimensions: List[String] = List("0", "1", "2"), dimensionName: String = "Étage",
                                 geometryColumn: String = "geom",
                                 dbGroup: String = "ji_2022", style: Option[Styles.Style] = None) extends Layer {
    def tblName(dimension: String) = s"${dbGroup}.${tablePrefix}_$dimension"
  }

  case class MultiDimensionWMSLayer(params: Map[String, String], title: String, defaultVisibility: Boolean = true, url: String = "https://geoportail.epfl.ch/prod/wsgi/mapserv_proxy?VERSION=1.3.0&floor={dimension}", dimensions: List[String] = List("0", "1", "2"), dimensionName: String = "Étage") extends Layer {
    private val fullParams: Map[String, String] = Map("TILED" -> "true", "VERSION" -> "1.3.0") ++ params

    def forDimension(dim: String): MultiDimensionWMSLayer =
      copy(params = fullParams.view.mapValues(v => v.replaceAll("\\{dimension}", dim)).toMap, url = url.replaceAll("\\{dimension}", dim), dimensions = List(dim))
  }

  case class LayerGroup[T <: Layer](name: String, layers: List[T])

  case class Feature(geometry: Geometry, layer: String, dimension: String, data: Map[String, String], id: Int)

  case class LayerData(title: String, tableName: String, features: List[Feature], style: Option[Styles.Style] = None) extends Layer

  implicit val geomWrites: Writes[Geometry] = new Writes[Geometry] {
    override def writes(o: Geometry): JsValue = o match {
      case poly: Polygon =>
        val points = (0 until poly.numPoints()) map (poly.getPoint)
        Json.obj("type" -> JsString("polygon"), "points" -> JsArray(points.map(writes)))
      case point: Point => Json.obj("type" -> JsString("point"), "x" -> JsNumber(point.x), "y" -> JsNumber(point.y), "z" -> JsNumber(point.z))
    }
  }
  implicit val featureWrite: OWrites[Feature] = Json.writes[Feature]
  implicit val layerDataWrite: OWrites[LayerData] = Json.writes[LayerData]
  implicit val multiDimensionWMSLayerWrite: OWrites[MultiDimensionWMSLayer] = Json.writes[MultiDimensionWMSLayer]
  implicit val layerGroupWrite: OWrites[LayerGroup[MultiDimensionWMSLayer]] = Json.writes[LayerGroup[MultiDimensionWMSLayer]]
  implicit val layerGroupDataWrite: OWrites[LayerGroup[LayerData]] = Json.writes[LayerGroup[LayerData]]

  val Layers: List[LayerGroup[MultiDimensionLayer]] = List(
    LayerGroup("Sécurité", List(
      MultiDimensionLayer("barrieres", List("type", "commentaires"), Some("Barrières")),
      MultiDimensionLayer("postes_securitas", List("id_pj", "mission", "type_agent", "commentaires"), Some("Agents de sécurité")),

    )),
    LayerGroup("Décoration", List(
      MultiDimensionLayer("deco_baches", List("nombre", "motif"), Some("Baches")),
      MultiDimensionLayer("deco_elts_divers", List("nom", "description"), Some("Éléments divers")),
      MultiDimensionLayer("deco_tissus", List("longueur", "motifs", "no_casiers"), Some("Tissus")),
      MultiDimensionLayer("deco_zones", List("couleurs", "theme"), Some("Zones"))
    )),
    LayerGroup("Elec/Gaz", List(
      MultiDimensionLayer("gaz_besoins", List("nom", "besoin", "commentaires"), Some("Besoins Gaz")),
      MultiDimensionLayer("gaz_stockage", List("nom", "commentaires"), Some("Stockage Gaz")),
    )),
    LayerGroup("Staff", List(
      MultiDimensionLayer("postes_staffs", List("nom_poste", "type_poste", "niveau_min", "nb_staffs", "commentaires"), Some("Postes Staffs"))
    )),
    LayerGroup("Rallye", List(
      MultiDimensionLayer("rallye", List("id"), Some("Panneaux rallye"))
    )),
    LayerGroup("Pro", List(
      MultiDimensionLayer("stands", List("id_pj", "exposant", "type", "prix", "nb_tables", "nb_chaises", "nb_panneaux", "commentaires", "puissance_elec_requise"), style = Some(
        UnionStyle(List(AttributeBasedStyle("type", ColorFillStyle("rgba(164,113,88,1.0)"), Map(
          "Associatif" -> ColorFillStyle("rgba(90,211,96,1.0)"),
          "Accueil" -> ColorFillStyle("rgba(213,163,48,1.0)"),
          "Bar" -> ColorFillStyle("rgba(107,149,30,1.0)"),
          "Commercial" -> ColorFillStyle("rgba(228,68,116,1.0)"),
          "Jeune Créateur" -> ColorFillStyle("rgba(132,77,127,1.0)"),
          "Contrepartie Prestataire/Invité" -> ColorFillStyle("rgba(156,122,37,1.0)"),
        )), LabelTextStyle("Stand {id_pj}\nExposant: {exposant}\n{nb_tables} tables", offsetX = -35)))
      )),
      MultiDimensionLayer("salles", List("nom_public", "type", "commentaires", "puissance_elec_requise", "max_personnes"), style = Some(
        UnionStyle(List(AttributeBasedStyle("type", ColorFillStyle("rgba(209,86,33,1.0)"), Map(
          "Activités Continues" -> ColorFillStyle("rgba(34,85,225,1.0)"),
          "Autre (préciser)" -> ColorFillStyle("rgba(92,229,38,1.0)"),
          "Concerts" -> ColorFillStyle("rgba(173,71,105,1.0)"),
          "Conférences" -> ColorFillStyle("rgba(198,103,207,1.0)"),
          "Stockage" -> ColorFillStyle("rgba(224,78,20,1.0)"),
        )), LabelTextStyle("Salle {nom_public}\n{type}\nMax {max_personnes} personnes")))
      ))
    ))
  )

  val BaseLayers: List[LayerGroup[MultiDimensionWMSLayer]] = List(
    LayerGroup("Batiments", List(
      MultiDimensionWMSLayer(Map("LAYERS" -> "grost{dimension}"), "Aires"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "batiments{dimension}"), "Bâtiments"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "locaux_l_01"), "Noms de salles"),
    )),
    LayerGroup("Indications GeoPortail", List(
      MultiDimensionWMSLayer(Map("LAYERS" -> "circulation{dimension}"), "Circulation"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "velo"), "Emplacements vélo"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "bancomats"), "Bancomats"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "wifi"), "Couverture WiFi", false),
      MultiDimensionWMSLayer(Map("LAYERS" -> "cablage"), "Cablage Network", false),
      MultiDimensionWMSLayer(Map("LAYERS" -> "cablage_dhcp"), "Cablage Network DHCP", false),
      MultiDimensionWMSLayer(Map("LAYERS" -> "imprimantes"), "Imprimantes", false),
      MultiDimensionWMSLayer(Map("LAYERS" -> "polynex"), "Ecrans PolyNex", false),
    ))
  )
}
