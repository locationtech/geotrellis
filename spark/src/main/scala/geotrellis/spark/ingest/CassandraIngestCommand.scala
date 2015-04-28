package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.CassandraArgs
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._

import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector._
import geotrellis.proj4._

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import com.datastax.spark.connector.cql.CassandraConnector

import scala.reflect.ClassTag

class CassandraIngestArgs extends IngestArgs with CassandraArgs {
  @Required var table: String = _
}

object CassandraIngestCommand extends ArgMain[CassandraIngestArgs] with Logging {
  def main(args: CassandraIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")

    val sparkConf = sparkContext.getConf
    val hadoopConf = sparkContext.hadoopConfiguration
    sparkConf.set("spark.cassandra.connection.host", args.host)


    Cassandra.withSession(args.host, args.keyspace) { implicit session =>
      val source = sparkContext.hadoopGeoTiffRDD(args.inPath).repartition(args.partitions)

      val layoutScheme = ZoomedLayoutScheme(256)
      val writer = CassandraRasterCatalog().writer[SpatialKey](RowMajorKeyIndexMethod, args.table)

      Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) =>
        writer.write(LayerId(args.layerName, level.zoom), rdd)
      }
    }
  }
}
