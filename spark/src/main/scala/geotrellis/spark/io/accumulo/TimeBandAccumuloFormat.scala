package geotrellis.spark.io.accumulo

import geotrellis.raster.{Tile, CellType, ArrayTile}
import geotrellis.spark._
import geotrellis.spark.ingest.IngestNetCDF.TimeBandTile
import geotrellis.spark.tiling._
import geotrellis.spark.rdd.{LayerMetaData, RasterRDD}
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.accumulo.core.util.{Pair => JPair}
import scala.collection.JavaConversions._

object TimeBandAccumuloFormat extends AccumuloFormat[TimeBandTile] {
  def rowId(id: TileId, md: LayerMetaData) = new Text(s"${md.level.id}_${id}")
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)

  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(raster: RasterRDD[TimeBandTile], layer: String): RDD[(Text, Mutation)] =
    raster.map{ case (TimeBandTile(tileId, time), tile) =>
      val mutation = new Mutation(rowId(tileId, raster.metaData))
      mutation.put(
        new Text(layer), new Text(time.toString),
        System.currentTimeMillis(),
        new Value(tile.toBytes()))
      (null, mutation)
    }

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[TimeBandTile] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(zoom, id) = key.getRow.toString
        val time = key.getColumnQualifier.toString.toDouble
        val tile = ArrayTile.fromBytes(value.get,
          metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
        TimeBandTile(id.toLong, time) -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }

  def setZoomBounds(job: Job, metaData: LayerMetaData): Unit = {
    val range = new ARange(new Text(s"${metaData.level.id}_0"), new Text(s"${metaData.level.id}_9")) :: Nil
    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, metaData: LayerMetaData, filters: Seq[AccumuloFilter]): Unit = {
    var tileBoundSet = false
    filters.foreach{
      case SpaceFilter(bounds, scheme) =>
        tileBoundSet = true
        val ranges = metaData.transform
          .withCoordScheme(scheme)
          .tileToIndex(bounds)
          .spans
          .map { ts => new ARange(rowId(ts._1, metaData), rowId(ts._2, metaData)) }
        InputFormatBase.setRanges(job, ranges)

      case TimeFilter(startTime, endTime) =>
        val pair = new JPair(new Text(startTime.toString), new Text(endTime.toString))
        InputFormatBase.fetchColumns(job, pair :: Nil)
    }
    if (! tileBoundSet) setZoomBounds(job, metaData)
  }
}
