package geotrellis.spark.io.accumulo

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Range => ARange, Key, Value, Mutation}
import org.apache.accumulo.core.util.{Pair => JPair}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import scala.collection.JavaConversions._
import scala.collection.mutable

object TimeRasterAccumuloDriver extends AccumuloDriver[TimeTileId] {
  def rowId(id: TileId, md: LayerMetaData) = new Text(s"${md.level.id}_${id}")

  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(raster: RasterRDD[TimeTileId], layer: String): RDD[(Text, Mutation)] =
    raster.map {
      case (TimeTileId(tileId, time), tile) =>
        val mutation = new Mutation(rowId(tileId, raster.metaData))
        mutation.put(new Text(layer), new Text(time.toString), System.currentTimeMillis(), new Value(tile.toBytes()))
        (null, mutation)
    }

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[TimeTileId] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(zoom, id) = key.getRow.toString
        val time = key.getColumnQualifier.toString.toDouble
        val tile = ArrayTile.fromBytes(value.get, metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
        TimeTileId(id.toLong, time) -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }

  def setZoomBounds(job: Job, metaData: LayerMetaData): Unit = {

    val range = new ARange(new Text(s"${metaData.level.id}_0"), new Text(s"${metaData.level.id}_9")) :: Nil
    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, layer: String, metaData: LayerMetaData, filters: Seq[KeyFilter]): Unit = {
    var tileBoundSet = false
    filters.foreach {
      case SpaceFilter(bounds, scheme) =>
        tileBoundSet = true
        val ranges = metaData.transform.withCoordScheme(scheme).tileToIndex(bounds).spans.map {
          ts => new ARange(rowId(ts._1, metaData), rowId(ts._2, metaData))
        }
        InputFormatBase.setRanges(job, ranges)
      case TimeFilter(startTime, endTime) =>
        val from = new JPair(new Text(layer), new Text(startTime.toString))
        val to = new JPair(new Text(layer), new Text(endTime.toString))

        val props = mutable.HashMap("startBound" -> startTime.toString, "endBound" -> endTime.toString,
          "startInclusive" -> "true", "endInclusive" -> "true")
        val iterator = new IteratorSetting(1, "TimeColumnFilter",
          "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props)
        InputFormatBase.addIterator(job, iterator)
    }
    if (!tileBoundSet) setZoomBounds(job, metaData)
    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new JPair(new Text(layer), null: Text) :: Nil)
  }
}