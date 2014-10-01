package geotrellis.spark.io.accumulo

import geotrellis.raster.{Tile, CellType, ArrayTile}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd.{LayerMetaData, RasterRDD}
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.accumulo.core.util.{Pair => JPair}
import scala.collection.JavaConversions._

/** Format to be used when Raster RDD is indexed only by TileId */
object FlatAccumuloFormat extends AccumuloFormat[TileId] {
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  def rowId(id: TileId, md: LayerMetaData) = new Text(s"${md.level.id}_${id}")

  def encode(raster: RasterRDD[TileId], layer: String): RDD[(Text, Mutation)] =
    raster.map{ case (id, tile) =>
      val mutation = new Mutation(rowId(id, raster.metaData))
      mutation.put(
        new Text(layer), null,
        System.currentTimeMillis(),
        new Value(tile.toBytes()))
      (null, mutation)
    }

  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[TileId] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(_, id) = key.getRow.toString
        val tile = ArrayTile.fromBytes(value.get,
          metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
        id.toLong -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }

  def setZoomBounds(job: Job, metaData: LayerMetaData): Unit = {
    val range = new ARange(
      new Text(s"${metaData.level.id}_0"),
      new Text(s"${metaData.level.id}_9")
    ) :: Nil

    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, layer: String, metaData: LayerMetaData, filters: Seq[AccumuloFilter]): Unit = {
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
    }
    if (! tileBoundSet) setZoomBounds(job, metaData)
    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new JPair(new Text(layer), null: Text) :: Nil)
  }
}