package geotrellis.spark.rdd
import geotrellis.DI
import geotrellis.spark._
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.spark.Logging
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD
import geotrellis.Raster
import geotrellis.spark.tiling.TileIdRaster

object SaveRasterFunctions extends Logging {

  // TODO - make this a typed function to avoid code duplication
  def save(raster: RDD[(TileIdWritable, ArgWritable)], path: String)(implicit n: DI) = {
    logInfo("Saving RasterWritableRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)
    raster.saveAsHadoopFile(path, classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)
    logInfo("End saving RasterWritableRDD out...")

  }

  def save(raster: RDD[(Long, Raster)], path: String)(implicit n: DI, o: DI) = {
    logInfo("Saving TileIdRasterRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    raster.mapPartitions(_.map(TileIdRaster.toTileIdArgWritable(_)), true)
      .saveAsHadoopFile(path, classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)
    logInfo("End saving RasterRDD out...")

  }
}