package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector._
import org.apache.hadoop.fs.Path
import org.apache.spark._
import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

class HadoopIngestArgs extends IngestArgs {
  @Required var catalog: String = _

  def catalogPath = new Path(catalog)
}

object HadoopIngestCommand extends ArgMain[HadoopIngestArgs] with Logging {
  def main(args: HadoopIngestArgs): Unit = {
   System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")
    val conf = sparkContext.hadoopConfiguration
    conf.set("io.map.index.interval", "1")

    val catalog = HadoopRasterCatalog(args.catalogPath)
    val source = sparkContext.hadoopGeoTiffRDD(args.inPath)
    val layoutScheme = ZoomedLayoutScheme()

    Ingest[ProjectedExtent, SpatialKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) => 
      catalog
        .writer[SpatialKey](args.clobber)
        .write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
