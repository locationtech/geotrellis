package geotrellis.spark.ingest

import geotrellis.raster.CellType
import geotrellis.spark._
import geotrellis.spark.cmd.args.{AccumuloArgs}
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import geotrellis.spark.utils.SparkUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.apache.spark._
import com.quantifind.sumac.ArgMain
import org.apache.spark.rdd.PairRDDFunctions

import scala.reflect.ClassTag

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object NetCDFIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
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

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val source = sparkContext.netCdfRDD(args.inPath)
    val layoutScheme = ZoomedLayoutScheme()
    val (level, rdd) =  Ingest[NetCdfBand, SpaceTimeKey](source, args.destCrs, layoutScheme)

    val save = { (rdd: RasterRDD[SpaceTimeKey], level: LayoutLevel) =>
      accumulo.catalog.save(LayerId(args.layerName, level.zoom), args.table, rdd, args.clobber)
    }

    if (args.pyramid) {
      Pyramid.saveLevels(rdd, level, layoutScheme)(save)
    } else{
      save(rdd, level)
    }
  }
}
