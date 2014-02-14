package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.TileIdRaster
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.SequenceFileInputFormat
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.HadoopRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.TaskContext
import org.apache.spark.Partition
import geotrellis.Raster
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

  @transient val rasterPath = new Path(raster)
  @transient val pyramidPath = rasterPath.getParent()
  val zoom = rasterPath.getName().toInt
  val meta = PyramidMetadata(pyramidPath, sc.hadoopConfiguration)

}

object RasterHadoopRDD {

  final val SeqFileGlob = "/*[0-9]*/data"

  def apply(raster: String, sc: SparkContext) =
    new RasterHadoopRDD(raster, sc, sc.defaultMinSplits)

  def apply(raster: Path, sc: SparkContext) =
    new RasterHadoopRDD(raster.toUri().toString(), sc, sc.defaultMinSplits)

  def toRasterRDD(raster: String, sc: SparkContext): RasterRDD = {
    val rasterPath = new Path(raster)
    val rhd = apply(raster, sc)
    val (meta, zoom) = (rhd.meta, rhd.zoom)
    rhd.mapPartitions(_.map(TileIdRaster(_, meta, zoom)), true).withMetadata(meta)
  }

  def toRasterRDD(raster: Path, sc: SparkContext): RasterRDD =
    toRasterRDD(raster.toUri().toString(), sc)
}