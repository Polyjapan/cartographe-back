package qgis.projects

import java.io.InputStream
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import scala.xml.XML
import java.io.InputStreamReader

class QgzParser {
  /*
  String fileZip = "src/main/resources/unzipTest/compressed.zip";
        File destDir = new File("src/main/resources/unzipTest");
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
           // ...
        }
        zis.closeEntry();
        zis.close();
   */

  def parseFile(path: String) = {
    val is = new ZipInputStream(new FileInputStream(path))
    var entry = is.getNextEntry
    while (entry != null) {
      if (entry.getName endsWith "qgs") {
        parseQgs(is)
      }
      entry = is.getNextEntry
    }
    is.closeEntry()
    is.close()
  }

  private def parseQgs(qgs: InputStream) = {
    val xml = XML.load(qgs)
    val layers = (xml \\ "qgis" \\ "projectlayers" \\ "maplayer").map(elt => (elt \\ "id").text -> elt).toMap
    layers.map {
      case (k, v) => k -> v \ "renderer-v2"
    } map {
      case (k, v) =>
        // TODO: WRONG! Actually, the key has a `symbol` attribute that maps to `symbol.name`
        val cat = (v \ "categories" \ "category")
        val rules = (v \ "rules" \ "rule")
        val keys = if (cat.nonEmpty) cat else rules
        k -> keys.zip(v \ "symbols" \ "symbol")
    } filter {
      case (_, v) => v.nonEmpty
    } map {
      case (k, v) =>
        k -> (v map {
          case (key, symbol) =>
            key -> (symbol \ "layer" \ "prop").map(e => (e \@ "k") -> (e \@ "v"))
        })
    }

    // class: layers.values.toList(1).head._2 \ "layer" \@ "class"
    // props: layers.values.toList(1).head._2 \ "layer" \  "prop"
    // rule: rule \@ "filter" (is a QGis expression)
  }
}
