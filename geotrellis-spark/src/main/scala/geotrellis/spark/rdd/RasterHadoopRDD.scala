package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.TileIdRaster

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.NewHadoopRDD
/*
 * An RDD abstraction of rasters in Spark. This can give back either tuples of either
 * (TileIdWritable, ArgWritable) or (Long, Raster), the latter being the deserialized 
 * form of the former. See companion object 
 */
class RasterHadoopRDD private (raster: Path, sc: SparkContext, conf: Configuration)
  extends NewHadoopRDD[TileIdWritable, ArgWritable](
    sc,
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable],
    conf) {

  /*
   * Overriding the partitioner with a TileIdPartitioner 
   */
  override val partitioner = Some(TileIdPartitioner(raster, conf))

  @transient val pyramidPath = raster.getParent()
  val zoom = raster.getName().toInt
  val meta = PyramidMetadata(pyramidPath, conf)

}

object RasterHadoopRDD {

  final val SeqFileGlob = "/*[0-9]*/data"

  /* raster - fully qualified path to the raster (with zoom level)
   * 	e.g., file:///tmp/mypyramid/10 or hdfs:///geotrellis/images/mypyramid/10
   *   
   * sc - the spark context
   */
  def apply(raster: String, sc: SparkContext): RasterHadoopRDD =
    apply(new Path(raster), sc)

  def apply(raster: Path, sc: SparkContext): RasterHadoopRDD = {
    val job = new Job(sc.hadoopConfiguration)    
    val globbedPath = new Path(raster.toUri().toString() + SeqFileGlob)
    FileInputFormat.addInputPath(job, globbedPath)
    val updatedConf = job.getConfiguration
    new RasterHadoopRDD(raster, sc, updatedConf)
  }

  def toRasterRDD(raster: String, sc: SparkContext): RasterRDD = 
    toRasterRDD(new Path(raster), sc)

  def toRasterRDD(raster: Path, sc: SparkContext): RasterRDD = {
    val rhd = apply(raster, sc)
    val (meta, zoom) = (rhd.meta, rhd.zoom)
    rhd.mapPartitions(_.map(TileIdRaster(_, meta, zoom)), true).withMetadata(meta)
    
  }
}