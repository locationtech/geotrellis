package geotrellis.spark.io

import geotrellis.spark.rdd._

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import geotrellis.raster._
import geotrellis.vector.Extent

import geotrellis.proj4._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.spark.Logging
import org.apache.commons.codec.binary.Base64

import java.io.PrintWriter
import java.nio.ByteBuffer

package object hadoop {
  /** Upgrades a string to a Hadoop Path URL. Seems dangerous, but not surprising. */
  implicit def stringToPath(path: String): Path = new Path(path)

  implicit class HadoopSparkContextWrapper(sc: SparkContext) {
    def hadoopRasterRDD(path: String): TmsRasterRDD =
      hadoopRasterRDD(new Path(path))

    def hadoopRasterRDD(path: Path): TmsRasterRDD =
      RasterHadoopRDD(path, sc).toRasterRDD

    def hadoopGeoTiffRDD(path: String): RDD[((Extent, CRS), Tile)] =
      hadoopGeoTiffRDD(new Path(path))

    def hadoopGeoTiffRDD(path: Path): RDD[((Extent, CRS), Tile)] = {
      val updatedConf =
        sc.hadoopConfiguration.withInputDirectory(path)

      sc.newAPIHadoopRDD(
        updatedConf,
        classOf[GeotiffInputFormat],
        classOf[(Extent, CRS)],
        classOf[Tile]
      )
    }

    def hadoopGdalRDD(path: Path): RDD[(GdalRasterInfo, Tile)] = {
      val updatedConf =
        sc.hadoopConfiguration.withInputDirectory(path)

      sc.newAPIHadoopRDD(
        updatedConf,
        classOf[GdalInputFormat],
        classOf[GdalRasterInfo],
        classOf[Tile]
      )
    }

  }

  implicit class SavableRasterRDD(val rdd: TmsRasterRDD) extends Logging {
    def toWritable =
      rdd.mapPartitions({ partition =>
        partition.map(_.toWritable)
      }, true)


    def saveAsHadoopRasterRDD(path: String): Unit =
      saveAsHadoopRasterRDD(new Path(path))

    def saveAsHadoopRasterRDD(path: Path) = {
      val conf = rdd.context.hadoopConfiguration

      logInfo("Saving RasterRDD out...")
      val jobConf = new JobConf(conf)
      jobConf.set("io.map.index.interval", "1");
      SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

      val writableRDD: RDD[WritableTile] =
        rdd.sortByKey().map(TmsTile(_).toWritable)

      writableRDD.saveAsHadoopFile(
        path.toUri().toString(),
        classOf[TileIdWritable],
        classOf[ArgWritable],
        classOf[MapFileOutputFormat],
        jobConf)

      logInfo(s"Finished saving raster to ${path}")

      rdd.partitioner match {
        case Some(partitioner) =>
          partitioner match {
            case p: TileIdPartitioner =>
              HadoopUtils.writeSplits(p.splits, path, conf)
            case _ =>
          }
        case _ =>
      }

      HadoopUtils.writeLayerMetaData(rdd.metaData, path, rdd.context.hadoopConfiguration)

      logInfo(s"Finished saving ${path}")
    }
  }

  implicit class TmsTileWrapper(tmsTile: TmsTile) {
    def toWritable(): WritableTile =
      (TileIdWritable(tmsTile.id), ArgWritable.fromTile(tmsTile.tile))
  }
}
