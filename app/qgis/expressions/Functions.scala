package qgis.expressions

object Functions {
  type Value = Any // TODO

  trait Function {
    val numArgsMin: Int
    val numArgsMax: Int
    val name: String
    def apply(args: List[Value]): Value
  }

  private def _fn(name: String, argsMin: Int, argsMax: Int, applyFn: (List[Value] => Value)): (String, Function) = {
    name -> new Function {
      override val numArgsMin: Int = argsMin
      override val numArgsMax: Int = argsMax
      override val name: String = name
      override def apply(args: List[Value]): Value = applyFn(args)
    }
  }

  private implicit def funcToPair(f: Function): (String, Function) = f.name -> f

  // https://github.com/qgis/QGIS/blob/master/src/core/expression/qgsexpressionfunction.cpp#L6511
  private val functions: Map[String, Function] = Map(
    // _fn("x_min", )
  )

  def apply(name: String): Option[Function] = functions.get(name)

}
