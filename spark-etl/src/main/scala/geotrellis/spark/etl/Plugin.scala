package geotrellis.spark.etl

trait Plugin {
  type Parameters = Map[String, String]
  def requiredKeys: Array[String]
}
