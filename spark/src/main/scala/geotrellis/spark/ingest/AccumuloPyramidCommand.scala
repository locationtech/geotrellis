package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.spark._
import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

class AccumuloPyramidArgs extends AccumuloArgs {
  @Required var layerName: String = _
  @Required var table: String = _
  @Required var startLevel: Int = _
}

object AccumuloPyramidCommand extends ArgMain[AccumuloPyramidArgs] with Logging {
  def main(args: AccumuloPyramidArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")

    implicit val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val catalog = AccumuloRasterCatalog()

    val reader = catalog.reader[SpatialKey]
    val writer = catalog.writer[SpatialKey](args.table)

    val rdd = reader.read(LayerId(args.layerName, args.startLevel))

    val layoutScheme = ZoomedLayoutScheme(256)
    val level = layoutScheme.levelFor(args.startLevel)

    val save = { (rdd: RasterRDD[SpatialKey], level: LayoutLevel) =>
      writer.write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}
