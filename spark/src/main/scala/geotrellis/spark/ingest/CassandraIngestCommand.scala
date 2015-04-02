package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.CassandraArgs
import geotrellis.spark.io.cassandra._

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

    val connector = CassandraConnector(sparkConf)
    val cassandra = CassandraInstance(connector, args.keyspace)

    val source = sparkContext.hadoopGeoTiffRDD(args.inPath).repartition(args.partitions)

    val layoutScheme = ZoomedLayoutScheme(256)
    val (level, rdd) =  Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme)

    val save = { (rdd: RasterRDD[SpatialKey], level: LayoutLevel) =>
      cassandra.catalog.save(LayerId(args.layerName, level.zoom), args.table, rdd, args.clobber)
    }
    if (args.pyramid) {
      Pyramid.saveLevels(rdd, level, layoutScheme)(save)
    } else{
      save(rdd, level)
    }
  }
}
