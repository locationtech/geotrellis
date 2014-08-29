package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.proj4._
import org.apache.accumulo.core.client.ZooKeeperInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._
import org.apache.spark.rdd._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required
import geotrellis.spark.io.accumulo._

class AccumuloIngestArgs extends IngestArgs {
  @Required var table: String = _
  @Required var layer: String = _
  @Required var zookeeper: String = _
  @Required var instance: String = _
  @Required var user: String = _
  @Required var password: String = _

}

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: AccumuloIngestArgs): Unit = {
    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)

    val sourceCRS = LatLng
    val destCRS = LatLng

    val accumulo = AccumuloInstance(
      args.instance, args.zookeeper, args.user, new PasswordToken(args.password))

//    val table = AccumuloTmsTable(args.table, accumulo.connector)
    implicit val format = new TmsTilingAccumuloFormat


    implicit val sparkContext = args.sparkContext("Ingest")
    try {
      val source = sparkContext.hadoopGeoTiffRDD(inPath)
      val sink = { (tiles: RDD[TmsTile], metaData: LayerMetaData) =>
        val raster: RasterRDD = new RasterRDD(tiles, metaData)

        raster.saveAccumulo(accumulo.connector)(args.table,  TmsLayer(args.layer, metaData.level.id))(format)

        logInfo(s"Saved raster to accumulo table: ${args.table}.")
      }

      Ingest(sparkContext)(source, sink, sourceCRS, destCRS, TilingScheme.TMS)

    } finally {
      sparkContext.stop
    }
  }
}