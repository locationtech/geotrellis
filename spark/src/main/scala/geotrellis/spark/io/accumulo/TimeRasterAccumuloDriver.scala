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

object TimeRasterAccumuloDriver extends AccumuloDriver[TimeSpatialKey] {
  def rowId(layerId: LayerId, id: SpatialKey) = new Text(s"${layerId.zoom}_${id}")
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(SpatialKey)

  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(layerId: LayerId, raster: RasterRDD[TimeSpatialKey]): RDD[(Text, Mutation)] =
    raster.map {
      case (TimeSpatialKey(spatialKey, time), tile) =>
        val mutation = new Mutation(rowId(layerId, spatialKey))
        mutation.put(new Text(layerId.name), new Text(time.toString), System.currentTimeMillis(), new Value(tile.toBytes()))
        (null, mutation)
    }

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: RasterMetaData): RasterRDD[TimeSpatialKey] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(zoom, id) = key.getRow.toString
        val time = key.getColumnQualifier.toString.toDouble
        val tile = ArrayTile.fromBytes(value.get, metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
        TimeSpatialKey(id.toLong, time) -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }

  def setZoomBounds(job: Job, metaData: LayerMetaData): Unit = {

    val range = new ARange(new Text(s"${metaData.id.zoom}_0"), new Text(s"${metaData.id.zoom}_9")) :: Nil
    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, layerMetaData: LayerMetaData, filters: Seq[KeyFilter]): Unit = {
    val LayerMetaData(layerId, rasterMetaData) = layerMetaData
    var tileBoundSet = false
    filters.foreach {
      case SpaceFilter(bounds, scheme) =>
        tileBoundSet = true
        val ranges = 
          rasterMetaData.transform.withCoordScheme(scheme).tileToIndex(bounds).spans
            .map { case (startKey, endKey) => 
              new ARange(rowId(layerId, startKey), rowId(layerId, endKey))
             }

        InputFormatBase.setRanges(job, ranges)
      case TimeFilter(startTime, endTime) =>
        val from = new JPair(new Text(layerId.name), new Text(startTime.toString))
        val to = new JPair(new Text(layerId.name), new Text(endTime.toString))

        val props = 
          Map(
            "startBound" -> startTime.toString, 
            "endBound" -> endTime.toString,
            "startInclusive" -> "true",
            "endInclusive" -> "true"
          )

        val iterator = 
          new IteratorSetting(1, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props)
        InputFormatBase.addIterator(job, iterator)
    }
    if (!tileBoundSet) setZoomBounds(job, layerMetaData)
    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new JPair(new Text(layerId.name), null: Text) :: Nil)
  }
}
