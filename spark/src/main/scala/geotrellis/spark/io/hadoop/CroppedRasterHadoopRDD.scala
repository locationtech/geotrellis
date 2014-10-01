package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.rdd._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.tiling._
import geotrellis.raster._

import org.apache.spark._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._
import org.apache.hadoop.conf._

class CroppedRasterHadoopRDD private (
    path: Path, gridBounds: GridBounds, sc: SparkContext, conf: Configuration)
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
   * returns true if specific partition has TileIDs for the gridBounds
   */
  def includePartition(p: Partition): Boolean = {
    //test if partition range intersects with a set of row ranges
    def intersects(rows: Seq[(TileId, TileId)], partition: (TileId, TileId)): Boolean = {
      for (row <- rows) {
        if ( //If the row edges are in range or row fully includes the range
          (row._1 >= partition._1 && row._1 <= partition._1) ||
            (row._2 >= partition._1 && row._2 <= partition._2) ||
            (row._1 < partition._1 && row._2 > partition._2)
        ) return true
      }
      false
    }

    val (min, max) = partitioner.get.range(p.index)
    intersects(metaData.transform.gridToIndex(gridBounds).spans, (min, max))
  }

  /**
   * returns true if the specific TileID is in the extent
   */
  def includeKey(key: TileIdWritable): Boolean = metaData.transform.gridToIndex(gridBounds).contains(key.get)

  def toRasterRDD(): RasterRDD[TileId] =
    asRasterRDD(metaData) {
      map(_.toTuple(metaData))
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
  def apply(path: String, gridBounds: GridBounds, sc: SparkContext): CroppedRasterHadoopRDD =
    apply(new Path(path), gridBounds, sc)

  def apply(path: Path, gridBounds: GridBounds, sc: SparkContext): CroppedRasterHadoopRDD = {
    val updatedConf = sc.hadoopConfiguration.withInputPath(path.suffix(SeqFileGlob))
    new CroppedRasterHadoopRDD(path, gridBounds, sc, updatedConf)
  }
}
