package geotrellis.spark.rdd

import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.SequenceFileInputFormat
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.HadoopRDD
import geotrellis.spark.metadata.PyramidMetadata
import org.apache.hadoop.fs.Path
import geotrellis.Raster
import geotrellis.spark.tiling.TileIdRaster
import org.apache.spark.rdd.RDD

/*
 * An RDD abstraction of rasters in Spark. This can give back either tuples of either
 * (TileIdWritable, ArgWritable) or (Long, Raster), the latter being the deserialized 
 * form of the former. See companion object for apply and asTiledRaster respectively 
 * 
 * raster - fully qualified path to the raster (with zoom level)
 * 		e.g., file:///tmp/mypyramid/10 or hdfs:///geotrellis/images/mypyramid/10
 *   
 * sc - the spark context
 * minSplits - override the default number of partitions with this number, to get, say
 * 		more than the default number of partitions
 *   
 */
class RasterHadoopRDD(raster: String, sc: SparkContext, minSplits: Int)
  extends HadoopRDD[TileIdWritable, ArgWritable](
    sc,
    sc.broadcast(new SerializableWritable(sc.hadoopConfiguration)),
    Some((jobConf: JobConf) => FileInputFormat.setInputPaths(jobConf, raster + RasterHadoopRDD.SeqFileGlob)),
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable],
    minSplits) {

  /*
   * Overriding the partitioner with a TileIdPartitioner 
   */
  override val partitioner = Some(TileIdPartitioner(raster, sc.hadoopConfiguration))

}

object RasterHadoopRDD {

  final val SeqFileGlob = "/*[0-9]*/data"

  def apply( raster: String, sc: SparkContext): RDD[(TileIdWritable, ArgWritable)] =
    new RasterHadoopRDD(raster, sc, sc.defaultMinSplits)
  

  def asTileIdRaster(raster: String, sc: SparkContext): RDD[TileIdRaster] = {
    val rasterPath = new Path(raster)
    val meta = PyramidMetadata(rasterPath.getParent(), sc.hadoopConfiguration)
	apply(raster, sc).map(TileIdRaster.from(_, meta, rasterPath.getName().toInt))
  }

}