package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.json._
import spray.json._

trait AttributeCatalog {
  def load[T: JsonFormat](id: LayerId, attributeName: String): T
  def save[T: JsonFormat](id: LayerId, attributeName: String, value: T): Unit
  def listLayers: List[(LayerId, String)]
}

object AttributeCatalog {
  final val METADATA_FIELD = "metadata"
  final val TABLENAME_FIELD = "table"
  final val KEYCLASS_FIELD = "keyClass"
  final val HISTOGRAM_FIELD = "histogram"
}
