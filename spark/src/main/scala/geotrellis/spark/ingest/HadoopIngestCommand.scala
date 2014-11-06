package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.cmd.args._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.proj4._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd._

import java.io.PrintWriter

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

import spire.syntax.cfor._
import scala.reflect.ClassTag

class HadoopIngestArgs extends IngestArgs {
  @Required var catalog: String = _

  def catalogPath = new Path(catalog)
}

object HadoopIngestCommand extends ArgMain[HadoopIngestArgs] with Logging {
  def main(args: HadoopIngestArgs): Unit = {
   System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    implicit val sparkContext = args.sparkContext("Ingest")

    val catalog: HadoopCatalog = HadoopCatalog(sparkContext, args.catalogPath)
    val source = sparkContext.hadoopGeoTiffRDD(args.inPath)    
    val (layerMetaData, rdd) =  Ingest[ProjectedExtent, SpatialKey](source, args.layerName, args.destCrs, ZoomedLayoutScheme())

    if (args.pyramid) {
      ??? // TODO do pyramiding
    } else{
      catalog.save(layerMetaData.id, rdd)    
    }
  }
}


