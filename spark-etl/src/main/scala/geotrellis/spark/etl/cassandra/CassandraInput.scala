package geotrellis.spark.etl.cassandra

import geotrellis.spark.LayerId
import geotrellis.spark.etl.InputPlugin
import geotrellis.vector.Extent

trait CassandraInput extends InputPlugin {
  val name = "cassandra"
  val format = "catalog"

  val requiredKeys = Array("host", "keyspace", "table", "layer") // optional: bbox

  def parse(props: Map[String, String]): (LayerId, Option[Extent]) = {
    val bbox = props.get("bbox").map(Extent.fromString)
    val chunks = props("layer").split(":")
    val id = LayerId(chunks(0), chunks(1).toInt)
    (id, bbox)
  }
}
