package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args.AccumuloArgs
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.tiling._
import geotrellis.vector.Extent
import geotrellis.proj4._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs._

import org.apache.spark._

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import scala.reflect.ClassTag

class AccumuloIngestArgs extends IngestArgs with AccumuloArgs {
  @Required var table: String = _  
}

class AccumuloIngest[T: IngestKey, K: SpatialComponent: AccumuloDriver: ClassTag](catalog: AccumuloCatalog, layoutScheme: LayoutScheme)(implicit tiler: Tiler[T, K])
    extends Ingest[T, K](layoutScheme) {
  def save(layerMetaData: LayerMetaData, rdd: RasterRDD[K]): Unit =
    catalog.save(layerMetaData.id, rdd)
}

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

    val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val ingest = new AccumuloIngest[ProjectedExtent, SpatialKey](accumulo.catalog, ZoomedLayoutScheme())
    val source = sparkContext.hadoopGeoTiffRDD(args.inPath)

    ingest(source, args.layerName, args.destCrs)
  }
}
