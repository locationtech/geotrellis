package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.proj4._
import org.apache.accumulo.core.client.{Connector, ZooKeeperInstance}
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._
import org.apache.spark.rdd._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required
import geotrellis.spark.io.accumulo._

class AccumuloIngestArgs extends IngestArgs with AccumuloArgs {
  @Required var table: String = _
  @Required var layer: String = _
}

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {

  def accumuloSink(table: String, layer: String, connector: Connector): Ingest.Sink = {
    (tiles: RDD[TmsTile], metaData: LayerMetaData) =>
      //note: likely the storage index will need to be a parameter
      val format = new TmsTilingAccumuloFormat
      val raster: RasterRDD = new RasterRDD(tiles, metaData)

      raster.saveAccumulo(connector)(table,  TmsLayer(layer, metaData.level.id))(format)

      logInfo(s"Saved raster to accumulo table: ${table}.")
  }

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: AccumuloIngestArgs): Unit = {
    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)

    val sourceCRS = LatLng
    val destCRS = LatLng

    val instance = args.instance match {
      case "fake" => new MockInstance()
      case _ => new ZooKeeperInstance(args.instance, args.zookeeper)
    }
    val connector = instance.getConnector(args.user, new PasswordToken(args.password))

    implicit val sparkContext = args.sparkContext("Ingest")
    try {
      val source = sparkContext.hadoopGeoTiffRDD(inPath)
      val sink = accumuloSink(args.table, args.layer, connector)

      Ingest(sparkContext)(source, sink, sourceCRS, destCRS, TilingScheme.TMS)

    } finally {
      sparkContext.stop()
    }
  }
}