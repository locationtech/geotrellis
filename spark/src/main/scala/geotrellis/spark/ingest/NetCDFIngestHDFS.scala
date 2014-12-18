package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.ingest.NetCDFIngestCommand._
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.ingest._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import geotrellis.spark.utils.SparkUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.spark._
import com.quantifind.sumac.ArgMain

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object NetCDFIngestHDFSCommand extends ArgMain[HadoopIngestArgs] with Logging {
  def main(args: HadoopIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")
    val conf = sparkContext.hadoopConfiguration
    conf.set("io.map.index.interval", "1")

    implicit val tiler: Tiler[NetCdfBand, SpaceTimeKey] = {
      val getExtent = (inKey: NetCdfBand) => inKey.extent
      val createKey = (inKey: NetCdfBand, spatialComponent: SpatialKey) =>
        SpaceTimeKey(spatialComponent, inKey.time)

      Tiler(getExtent, createKey)
    }

    val catalog: HadoopCatalog = HadoopCatalog(sparkContext, args.catalogPath)
    val source = sparkContext.netCdfRDD(args.inPath)
    val layoutScheme = ZoomedLayoutScheme()
    val (level, rdd) =  Ingest[NetCdfBand, SpaceTimeKey](source, args.destCrs, layoutScheme, true)

    val save = { (rdd: RasterRDD[SpaceTimeKey], level: LayoutLevel) =>
      catalog.save(LayerId(args.layerName, level.zoom), rdd, true)
    }

    if (args.pyramid) {
      Pyramid.saveLevels(rdd, level, layoutScheme)(save)
    } else{
      save(rdd, level)
    }
  }
}
