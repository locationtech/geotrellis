package geotrellis.spark

package object etl {
  private [etl] def requireKeys(name: String, props: Map[String, String], keys: Seq[String]) = {
    val missing = keys
      .filterNot(props.contains)
      .mkString(", ")
    require(missing.isEmpty, s"$name module missing required settings: $missing")
  }
}
