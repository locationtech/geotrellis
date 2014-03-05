package geotrellis.spark.rdd

import geotrellis._
import geotrellis.spark._
import geotrellis.spark.formats._

import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.spark.Logging
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD
import org.apache.hadoop.fs.Path

object SaveRasterFunctions extends Logging {
    
  def save(raster: RDD[WritableTile], path: Path): Unit = {
    logInfo("Saving RasterWritableRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    raster.saveAsHadoopFile(path.toUri().toString(), 
                            classOf[TileIdWritable], 
                            classOf[ArgWritable], 
                            classOf[MapFileOutputFormat], 
                            jobConf)

    logInfo("End saving RasterWritableRDD out...")
  }

  def save(raster: RasterRDD, path: Path): Unit = {
    val zoom = path.getName().toInt
    val pyramidPath = path.getParent()

    logInfo("Saving RasterRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    val writableRDD =
      raster.mapPartitions({ partition =>
        partition.map { tile =>
          tile.toWritable
        }
      }, true)

    writableRDD.saveAsHadoopFile(path.toUri().toString(), 
                                 classOf[TileIdWritable], 
                                 classOf[ArgWritable], 
                                 classOf[MapFileOutputFormat], 
                                 jobConf)

    logInfo(s"Finished saving raster to ${path}")
    raster.opCtx.toMetadata.save(pyramidPath, raster.context.hadoopConfiguration)
    logInfo(s"Finished saving metadata to ${pyramidPath}")
  }
}
