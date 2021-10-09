package qgis.expressions

import org.specs2.mutable.Specification

class QgsCompilerTest extends Specification {
  private val testProgram = "concat(\"id_pj\", '\\n', IF(\"exposant\" IS NULL, '** LIBRE **', \"exposant\"), '\\n', IF(\"nb_tables\" IS NULL, '?', \"nb_tables\"), 't', IF(\"nb_chaises\" IS NULL, '?', \"nb_chaises\"), 'c', IF(\"nb_panneaux\" IS NULL, '?', \"nb_chaises\"), 'p')"

  "QgsCompilerTest" should {
    "apply" in {
      print(QgsCompiler("layer", testProgram))
      ok
    }
  }
}
