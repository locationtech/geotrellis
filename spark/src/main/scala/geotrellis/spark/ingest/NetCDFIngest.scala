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

class NetCdfIngest(catalog: AccumuloCatalog, layoutScheme: LayoutScheme)(implicit tiler: Tiler[NetCdfBand, SpaceTimeKey])
    extends AccumuloIngest[NetCdfBand, SpaceTimeKey](catalog, layoutScheme) {
  override def isUniform = true
}

object NetCDFIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))

    implicit val tiler =
      new Tiler[NetCdfBand, SpaceTimeKey] {
        def getExtent(inKey: NetCdfBand): Extent = inKey.extent
        def createKey(inKey: NetCdfBand, spatialComponent: SpatialKey): SpaceTimeKey = 
          SpaceTimeKey(spatialComponent, inKey.time)
      }

    val ingest = new NetCdfIngest(accumulo.catalog, ZoomedLayoutScheme())
    val source = sparkContext.netCdfRDD(args.inPath)

    ingest(source, args.layerName, args.destCrs)
  }
}
