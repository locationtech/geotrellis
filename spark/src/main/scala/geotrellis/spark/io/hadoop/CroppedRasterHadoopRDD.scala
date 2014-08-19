package geotrellis.spark.io.hadoop

import geotrellis.spark.rdd._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.tiling._

import org.apache.spark._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._
import org.apache.hadoop.conf._

class CroppedRasterHadoopRDD private (
    path: Path, extent: TileExtent, sc: SparkContext, conf: Configuration)
  extends PreFilteredHadoopRDD[TileIdWritable, ArgWritable](
    sc,
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable],
    conf)
{
  lazy val metaData = HadoopUtils.readLayerMetaData(path, context.hadoopConfiguration)

  override val partitioner = Some(TileIdPartitioner(HadoopUtils.readSplits(path, conf)))

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

    val (min, max) = partitioner.get.range(p.index)
    intersects(extent.rowRanges(metaData.zoomLevel), TileSpan(min, max))
  }

  /**
   * returns true if the specific TileID is in the extent
   */
  def includeKey(key: TileIdWritable): Boolean = extent.contains(metaData.zoomLevel)(key.get)

  def toRasterRDD(): RasterRDD =
    asRasterRDD(metaData) {
      map(_.toTmsTile(metaData))
    }
}

object CroppedRasterHadoopRDD {
  final val SeqFileGlob = "/*[0-9]*/data"

  /**
   *  @param path   Fully qualified path to the raster (with zoom level)
   * 	                  e.g. hdfs:///geotrellis/images/mypyramid/10
   *
   * @param  sc       The spark context
   */
  def apply(path: String, extent: TileExtent, sc: SparkContext): CroppedRasterHadoopRDD =
    apply(new Path(path), extent, sc)

  def apply(path: Path, extent: TileExtent, sc: SparkContext): CroppedRasterHadoopRDD = {
    val updatedConf = sc.hadoopConfiguration.withInputPath(path.suffix(SeqFileGlob))
    new CroppedRasterHadoopRDD(path, extent, sc, updatedConf)
  }
}
