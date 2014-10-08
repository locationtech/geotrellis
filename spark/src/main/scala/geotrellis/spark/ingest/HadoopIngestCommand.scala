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



class HadoopIngestArgs extends IngestArgs {
  @Required var outputpyramid: String = _
}
/**
  * @author akini
  *
  * Ingest GeoTIFFs into ArgWritable.
  *
  * Works in two modes:
  *
  * Local - all processing is done on a single node in RAM and not using Spark. Use this if
  * ingesting a single file or a bunch of files that do not overlap. Also, all files in
  * aggregate must fit in RAM. The non-overlapping constraint is due to there not being
  * any mosaicing in local mode
  *
  * Constraints:
  *
  * --input <path-to-tiffs> - this can either be a directory or a single tiff file and can either be in local fs or hdfs
  *
  * --outputpyramid <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://). If the directory
  * already exists, it is deleted
  *
  * --sparkMaster <spark-name>   i.e. local[10]
  *
  */
object HadoopIngestCommand extends ArgMain[HadoopIngestArgs] with Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: HadoopIngestArgs): Unit = {
    val conf = args.hadoopConf
    conf.set("io.map.index.interval", "1")

    val inPath = new Path(args.input)
    val outPath = new Path(args.outputpyramid)

    logInfo(s"Deleting and creating output path: $outPath")
    val outFs: FileSystem = outPath.getFileSystem(conf)
    outFs.delete(outPath, true)
    outFs.mkdirs(outPath)

    val destCRS = LatLng

    val sparkContext = args.sparkContext("Ingest")
    try {
      val source = sparkContext.hadoopGeoTiffRDD(inPath)
      val sink = { tiles: RasterRDD[SpatialKey] =>
        val metaData = tiles.metaData
        val partitioner = {
          val gridBounds = metaData.transform.mapToGrid(metaData.extent)
          val tileSizeBytes = gridBounds.width * gridBounds.height * metaData.cellType.bytes
          val blockSizeBytes = HdfsUtils.defaultBlockSize(inPath, conf)
          val splitGenerator =
            RasterSplitGenerator(gridBounds, metaData.transform, tileSizeBytes, blockSizeBytes)
          SpatialKeyPartitioner(splitGenerator.splits)
        }

        val outPathWithZoom = new Path(outPath, metaData.level.id.toString)
        tiles
          .partitionBy(partitioner)
          .toRasterRDD(metaData)
          .saveAsHadoopRasterRDD(outPathWithZoom)

        logInfo(s"Saved raster at zoom level ${metaData.level.id} to $outPathWithZoom")
      }

      Ingest(sparkContext)(source, sink, destCRS, TilingScheme.TMS)

    } finally {
      sparkContext.stop
    }
  }
}
