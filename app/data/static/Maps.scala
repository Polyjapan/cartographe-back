package data.static

import data.LayerDef.{LayerGroup, MapDef, MultiDimensionLayer, MultiDimensionWMSLayer}
import data.Styles.{AttributeBasedStyle, ColorFillStyle, LabelTextStyle, UnionStyle}
import services.MapsService

import scala.collection.mutable
import scala.concurrent.Future

object Maps extends MapsService {


  private val Maps: Map[Int, (MapDef, List[LayerGroup[MultiDimensionLayer]], List[LayerGroup[MultiDimensionWMSLayer]])] = {
    val map = mutable.Map[Int, (MapDef, List[LayerGroup[MultiDimensionLayer]], List[LayerGroup[MultiDimensionWMSLayer]])]()
    var nextId = 1

    def register(name: String, description: String, groups: Set[String], jsonLayers: List[LayerGroup[MultiDimensionLayer]], baseLayers: List[LayerGroup[MultiDimensionWMSLayer]]) = {
      val id = nextId
      nextId = nextId + 1
      map.put(id, (MapDef(id, name, description, groups), jsonLayers, baseLayers))
    }

    val PrivateJsonLayers: List[LayerGroup[MultiDimensionLayer]] = List(
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

    val PublicJsonLayers: List[LayerGroup[MultiDimensionLayer]] = List(
      LayerGroup("Plan Visiteurs", List(
      MultiDimensionLayer("stands", List("id_pj", "exposant", "type"), style = Some(
        UnionStyle(List(AttributeBasedStyle("type", ColorFillStyle("rgba(164,113,88,1.0)"), Map(
          "Associatif" -> ColorFillStyle("rgba(90,211,96,1.0)"),
          "Accueil" -> ColorFillStyle("rgba(213,163,48,1.0)"),
          "Bar" -> ColorFillStyle("rgba(107,149,30,1.0)"),
          "Commercial" -> ColorFillStyle("rgba(228,68,116,1.0)"),
          "Jeune Créateur" -> ColorFillStyle("rgba(132,77,127,1.0)"),
          "Contrepartie Prestataire/Invité" -> ColorFillStyle("rgba(156,122,37,1.0)"),
        )), LabelTextStyle("Stand {id_pj}\nExposant: {exposant}", offsetX = -35)))
      )),
      MultiDimensionLayer("salles", List("nom_public", "type"), style = Some(
        UnionStyle(List(AttributeBasedStyle("type", ColorFillStyle("rgba(209,86,33,1.0)"), Map(
          "Activités Continues" -> ColorFillStyle("rgba(34,85,225,1.0)"),
          "Autre (préciser)" -> ColorFillStyle("rgba(92,229,38,1.0)"),
          "Concerts" -> ColorFillStyle("rgba(173,71,105,1.0)"),
          "Conférences" -> ColorFillStyle("rgba(198,103,207,1.0)"),
          "Stockage" -> ColorFillStyle("rgba(224,78,20,1.0)"),
        )), LabelTextStyle("Salle {nom_public}\n{type}")))
      ))
    ))
    )

    val StaffJsonLayer = List(
      LayerGroup("Staff", List(
        MultiDimensionLayer("postes_staffs", List("nom_poste", "type_poste", "nb_staffs"), Some("Postes Staffs"))
      ))
    )

    val BaseBuildings: List[LayerGroup[MultiDimensionWMSLayer]] = List(LayerGroup("Batiments", List(
      MultiDimensionWMSLayer(Map("LAYERS" -> "grost{dimension}"), "Aires"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "batiments{dimension}"), "Bâtiments"),
      MultiDimensionWMSLayer(Map("LAYERS" -> "locaux_l_01"), "Noms de salles"),
    )))

    val BaseGeoPortalData: List[LayerGroup[MultiDimensionWMSLayer]] = List(
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

    register("Plan visiteurs", "Plan public à destination des visiteurs", Set(), PublicJsonLayers, BaseBuildings)
    register("Plan complet (comité)", "Plan complet à destination des membres du comité, contient des informations confidentielles", Set("comite-ji"), PrivateJsonLayers, BaseBuildings ++ BaseGeoPortalData)
    register("Plan staffs", "Plan à destination des membres du staff, contient le plan public ainsi que les postes staff", Set("comite-ji", "staff-ji"), PublicJsonLayers ++ StaffJsonLayer, BaseBuildings)

    map.toMap
  }

  /**
   * Gets all the available maps (must be filtered by groups)
   *
   * @return
   */
  override def getMaps: Future[List[MapDef]] = Future.successful(this.Maps.map(_._2._1).toList)

  /**
   * Gets the base layers of a given map (must be filtered by groups)
   *
   * @param mapId
   * @return
   */
  override def getBaseLayers(mapId: Int): Future[Option[(MapDef, List[LayerGroup[MultiDimensionWMSLayer]])]] = Future.successful(this.Maps.get(mapId).map(triple => (triple._1, triple._3)))

  /**
   * Gets the json layers of a given map (must be filtered by groups)
   *
   * @param mapId
   * @return
   */
override def getJsonLayers(mapId: Int): Future[Option[(MapDef, List[LayerGroup[MultiDimensionLayer]])]] = Future.successful(this.Maps.get(mapId).map(triple => (triple._1, triple._2)))
}
