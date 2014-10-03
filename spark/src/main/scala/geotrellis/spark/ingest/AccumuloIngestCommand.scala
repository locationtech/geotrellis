package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.proj4._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required
import geotrellis.spark.io.accumulo._

class AccumuloIngestArgs extends IngestArgs with AccumuloArgs {
  @Required var table: String = _
  @Required var layer: String = _
  var pyramid: Boolean = false
}

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {

  def accumuloSink(table: String, layer: String, catalog: AccumuloCatalog): Ingest.Sink = {
    (tiles: RasterRDD[TileId]) =>
      val raster= new RasterRDD(tiles, tiles.metaData)
      catalog.save(raster, layer, table)
      logInfo(s"Saved raster '$layer' to accumulo table: ${table}.")
  }

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: AccumuloIngestArgs): Unit = {
    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)

    val sourceCRS = LatLng
    val destCRS = LatLng

    implicit val sparkContext = args.sparkContext("Ingest")

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val catalog = accumulo.catalog

    try {
      val source = sparkContext.hadoopGeoTiffRDD(inPath)
      val sink = accumuloSink(args.table, args.layer, catalog)

      if (args.pyramid)
        Ingest(sparkContext)(source, Ingest.pyramid(sink), destCRS, TilingScheme.TMS)
      else
        Ingest(sparkContext)(source, sink, destCRS, TilingScheme.TMS)

    } finally {
      sparkContext.stop()
    }
  }
}