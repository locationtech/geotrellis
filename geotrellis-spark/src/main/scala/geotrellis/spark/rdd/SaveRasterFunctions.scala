package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.spark.Logging
import org.apache.spark.SparkContext.rddToPairRDDFunctions

object SaveRasterFunctions extends Logging {
  def save(raster: RasterWritableRDD, path: String) = {
    logInfo("Saving raster out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)
    raster.saveAsHadoopFile(path, classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)
    logInfo("End saving image out...")

  }
}