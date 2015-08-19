package geotrellis.spark.etl.accumulo

import geotrellis.spark.LayerId
import geotrellis.spark.etl.InputPlugin
import geotrellis.vector.Extent

trait AccumuloInput extends InputPlugin {

  def name = "accumulo"
  def format = "catalog"

  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table", "layer") // optional: bbox

  def parse(props: Map[String, String]): (LayerId, Option[Extent]) = {
    val bbox = props.get("bbox").map(Extent.fromString)
    val chunks = props("layer").split(":")
    val id = LayerId(chunks(0), chunks(1).toInt)
    (id, bbox)
  }

}