package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector._
import geotrellis.raster.Tile
import geotrellis.proj4._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import scala.reflect.ClassTag

class AccumuloIngestArgs extends IngestArgs with AccumuloArgs {
  @Required var table: String = _  
}

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")
    val conf = sparkContext.hadoopConfiguration
    conf.set("io.map.index.interval", "1")

    implicit val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))

    val source = sparkContext.hadoopGeoTiffRDD(args.inPath).repartition(args.partitions)
    val layoutScheme = ZoomedLayoutScheme(256)

    val writer = AccumuloRasterCatalog().writer[SpatialKey, Tile](RowMajorKeyIndexMethod, args.table)

    Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) => 
      writer.write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
