package geotrellis.spark.rdd

import geotrellis.spark.formats._
import geotrellis.spark.metadata._
import geotrellis.spark.tiling._

import org.apache.spark._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._
import org.apache.hadoop.conf._

class CroppedRasterHadoopRDD private (
    raster: Path, extent: TileExtent, sc: SparkContext, conf: Configuration)
  extends PreFilteredHadoopRDD[TileIdWritable, ArgWritable](
    sc,
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable],
    conf)
{
  /** Overriding the partitioner with a TileIdPartitioner */
  override val partitioner = Some(TileIdPartitioner(raster, conf))

  @transient val pyramidPath = raster.getParent()
  val zoom = raster.getName().toInt
  val meta = PyramidMetadata(pyramidPath, conf)

  /**
   * returns true if specific partition has TileIDs for extent
   */
  def includePartition(p: Partition): Boolean = {
    //test if partition range intersects with a set of row ranges
    def intersects(rows: Seq[TileSpan], partition: TileSpan): Boolean = {
      for (row <- rows) {
        if ( //If the row edges are in range or row fully includes the range
          (row.min >= partition.min && row.min <= partition.min) ||
            (row.max >= partition.min && row.max <= partition.max) ||
            (row.min < partition.min && row.max > partition.max)
        ) return true
      }
      false
    }

    val range = partitioner.get.range(p.index)
    intersects(extent.getRowRanges(zoom), TileSpan(range._1.get, range._2.get))
  }

  /**
   * returns true if the specific TileID is in the extent
   */
  def includeKey(key: TileIdWritable): Boolean = extent.contains(zoom)(key.get)

  def toRasterRDD(addUserNoData: Boolean = false): RasterRDD =
    mapPartitions { partition =>
      partition.map { writableTile =>
        writableTile.toTmsTile(meta, zoom, addUserNoData)
      }
    }
    .withContext(Context(zoom, meta, partitioner.get)) // .get is safe because it can't be 'None'
}

object CroppedRasterHadoopRDD {
  final val SeqFileGlob = "/*[0-9]*/data"

  /**
   *  @param raster   Fully qualified path to the raster (with zoom level)
   * 	                  e.g. hdfs:///geotrellis/images/mypyramid/10
   *
   * @param  sc       The spark context
   */
  def apply(raster: String, extent: TileExtent, sc: SparkContext): CroppedRasterHadoopRDD =
    apply(new Path(raster), extent, sc)

  def apply(raster: Path, extent: TileExtent, sc: SparkContext): CroppedRasterHadoopRDD = {
    val job = new Job(sc.hadoopConfiguration)
    val globbedPath = raster.suffix(SeqFileGlob)
    FileInputFormat.addInputPath(job, globbedPath)
    new CroppedRasterHadoopRDD(raster, extent, sc, job.getConfiguration)
  }
}