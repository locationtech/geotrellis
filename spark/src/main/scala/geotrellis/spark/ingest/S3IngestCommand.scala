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
import geotrellis.raster.Tile

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import scala.reflect.ClassTag

class S3IngestCommand extends IngestArgs {
  @Required var bucket: String = _  
  @Required var key: String = _ 
  var splitSize: Integer = 256
}

object S3IngestCommand extends ArgMain[S3IngestCommand] with Logging {
  def main(args: S3IngestCommand): Unit = {
    implicit val sc = SparkUtils.createSparkContext("S3 Ingest")
    
    val job = sc.newJob("geotiff-ingest")        
    S3InputFormat.setUrl(job, args.input)
    S3InputFormat.setMaxKeys(job, args.splitSize)

    val source = 
      sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])    
    
    val layoutScheme = ZoomedLayoutScheme(256)

    implicit val wProv = geotrellis.spark.io.s3.spatial.SpatialRasterRDDWriterProvider
    val catalog = S3RasterCatalog(args.bucket, args.key)      
    val writer = catalog.writer[SpatialKey]()
    
    Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) => 
      writer.write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
