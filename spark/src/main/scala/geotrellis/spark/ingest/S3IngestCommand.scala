package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector._
import geotrellis.proj4._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import scala.reflect.ClassTag

class S3IngestCommand extends IngestArgs with AccumuloArgs {
  @Required var bucket: String = _  
  @Required var key: String = _  
}

object S3IngestCommand extends ArgMain[S3IngestCommand] with Logging {
  def main(args: S3IngestCommand): Unit = {
    implicit val sparkContext = SparkUtils.createSparkContext("S3 Ingest")
    val conf = sparkContext.hadoopConfiguration
    val source = sparkContext.hadoopGeoTiffRDD(args.inPath).repartition(args.partitions)
    val layoutScheme = ZoomedLayoutScheme(256)

    implicit val wProv = geotrellis.spark.io.s3.spatial.SpatialRasterRDDWriterProvider
    val catalog = S3RasterCatalog(args.bucket, args.key)      
    val writer = catalog.writer[SpatialKey]()
    
    Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) => 
      writer.write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
