package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.formats._
import geotrellis.spark.metadata.Context
import geotrellis.spark.metadata.PyramidMetadata

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

  def toRasterRDD(): RasterRDD = 
    toRasterRDD(false)

  def toRasterRDD(addUserNoData: Boolean): RasterRDD = 
    mapPartitions { partition =>
      partition.map { writableTile =>
        writableTile.toTile(meta, zoom, addUserNoData)
      }
     }
    .withContext(Context.fromMetadata(zoom, meta))
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
}
