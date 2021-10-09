package qgis.expressions

object QgsCompilerMain {
  private val testProgram = "concat(\"id_pj\", '\\n', IF(\"exposant\" IS NULL, '** LIBRE **', \"exposant\"), '\\n', IF(\"nb_tables\" IS NULL, '?', \"nb_tables\"), 't', IF(\"nb_chaises\" IS NULL, '?', \"nb_chaises\"), 'c', IF(\"nb_panneaux\" IS NULL, '?', \"nb_chaises\"), 'p')"
  //private val testProgram = "IF(\"exposant\" IS NULL, '** LIBRE **', \"exposant\")"

  def main(args: Array[String]) = print(QgsCompiler("layer", testProgram))
}
