package geotrellis.spark.io


import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import geotrellis.raster._
import geotrellis.vector.Extent

import geotrellis.proj4._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.Logging
import org.apache.commons.codec.binary.Base64

import java.io.PrintWriter
import java.nio.ByteBuffer
import geotrellis.spark.tiling._

package object hadoop {
  /** Upgrades a string to a Hadoop Path URL. Seems dangerous, but not surprising. */
  implicit def stringToPath(path: String): Path = new Path(path)

  implicit class HadoopSparkContextWrapper(sc: SparkContext) {
    def hadoopRasterRDD(path: String): RasterRDD[SpatialKey] =
      hadoopRasterRDD(new Path(path))

    def hadoopRasterRDD(path: Path): RasterRDD[SpatialKey] =
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

    def gdalRDD(path: Path): RDD[(GdalRasterInfo, Tile)] = {
      val updatedConf = sc.hadoopConfiguration.withInputDirectory(path)

      sc.newAPIHadoopRDD(
        updatedConf,
        classOf[GdalInputFormat],
        classOf[GdalRasterInfo],
        classOf[Tile]
      )
    }

    def netCdfRDD(path: Path): RDD[(NetCdfBand, Tile)] = {
      gdalRDD(path)
        .map{ case (info, tile) =>
          val band = NetCdfBand(
            extent = info.file.rasterExtent.extent,
            crs = info.file.crs,
            varName = info.bandMeta("NETCDF_VARNAME"),
            time = info.bandMeta("NETCDF_DIM_Time").toDouble
          )
          band -> tile
        }
    }
  }

  implicit class SavableRasterRDD(val rdd: RasterRDD) extends Logging {
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
        rdd.sortByKey().map(_.toWritable)

      writableRDD.saveAsHadoopFile(
        path.toUri().toString(),
        classOf[SpatialKeyWritable],
        classOf[ArgWritable],
        classOf[MapFileOutputFormat],
        jobConf)

      logInfo(s"Finished saving raster to ${path}")

      rdd.partitioner match {
        case Some(partitioner) =>
          partitioner match {
            case p: SpatialKeyPartitioner =>
              HadoopUtils.writeSplits(p.splits, path, conf)
            case _ =>
          }
        case _ =>
      }

      HadoopUtils.writeLayerMetaData(rdd.metaData, path, rdd.context.hadoopConfiguration)

      logInfo(s"Finished saving ${path}")
    }
  }

  implicit class HadoopConfigurationWrapper(config: Configuration) {
    def withInputPath(path: Path): Configuration = {
      val job = Job.getInstance(config)
      FileInputFormat.addInputPath(job, path)
      job.getConfiguration
    }

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, config)
      HdfsUtils.putFilesInConf(allFiles.mkString(","), config)
    }
  }
}
