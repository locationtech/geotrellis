package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import org.apache.hadoop.fs._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.proj4.CRS

import org.apache.spark._
import org.apache.spark.rdd._

import com.quantifind.sumac.ArgMain

/**
 * Ingests raw multi-band NetCDF tiles into a re-projected and tiled RasterRDD
 */
object NetCDFIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

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
      accumulo.catalog.save(LayerId(args.layerName, level.zoom), rdd, args.table, true)
    }

    if (args.pyramid) {
      Pyramid.saveLevels(rdd, level, layoutScheme)(save).get // expose exceptions
    } else{
      save(rdd, level)
    }
  }
}
