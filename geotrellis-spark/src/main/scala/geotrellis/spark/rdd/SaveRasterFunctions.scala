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
import org.apache.hadoop.fs.Path

object SaveRasterFunctions extends Logging {
    
  def save(raster: RDD[TileIdArgWritable], path: Path): Unit = {
    logInfo("Saving RasterWritableRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)
    raster.saveAsHadoopFile(path.toUri().toString(), classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)
    logInfo("End saving RasterWritableRDD out...")

  }

    
  def save(raster: RasterRDD, path: Path): Unit = {
    val zoom = path.getName().toInt
    val pyramidPath = path.getParent()

    logInfo("Saving RasterRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    raster.mapPartitions(_.map(TileIdRaster.toTileIdArgWritable(_)), true)
      .saveAsHadoopFile(path.toUri().toString(), classOf[TileIdWritable], classOf[ArgWritable], classOf[MapFileOutputFormat], jobConf)

    logInfo(s"Finished saving raster to ${path}")
    raster.meta.save(pyramidPath, raster.context.hadoopConfiguration)
    logInfo(s"Finished saving metadata to ${pyramidPath}")
  }
}