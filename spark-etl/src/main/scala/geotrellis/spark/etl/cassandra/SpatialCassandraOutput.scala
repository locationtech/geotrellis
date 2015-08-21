package geotrellis.spark.etl.cassandra

import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index.KeyIndexMethod

import com.datastax.spark.connector.cql.CassandraConnector
import geotrellis.spark.utils.SparkUtils

import scala.reflect._

class SpatialCassandraOutput extends CassandraOutput {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {

    implicit val sparkContext = rdd.sparkContext
    Cassandra.withSession(props("host"), props("keyspace")) { implicit session =>
      CassandraRasterCatalog()
        .writer[SpatialKey](method.asInstanceOf[KeyIndexMethod[SpatialKey]], props("table"))
        .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
    }
  }
}
