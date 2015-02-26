package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._

import org.apache.spark._

class CassandraAttributeCatalog(sc: SparkContext, val attributeTable: String) extends AttributeCatalog {
  type ReadableWriteable[T] = RootJsonFormat[T]

  // Create the attribute table if it doesn't exist
  {
    
  }

  def load[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    // Load json-formatted attribute
  }

  def save[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    // Save json-formatted attribute
  }
}
