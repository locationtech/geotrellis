package geotrellis.spark.io.accumulo

import geotrellis.raster.{Tile, CellType, ArrayTile}
import geotrellis.spark._
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.accumulo.core.util.{Pair => JPair}
import scala.collection.JavaConversions._

object RasterAccumuloDriver extends AccumuloDriver[SpatialKey] {
  val rowIdRx = """(\d+)_(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  def rowId(layerId: LayerId, key: SpatialKey) = new Text(s"${layerId.zoom}_${key.col}_${key.row}")

  def encode(layerId: LayerId, raster: RasterRDD[SpatialKey]): RDD[(Text, Mutation)] =
    raster.map { case (key, tile) =>
      val mutation = new Mutation(rowId(layerId, key))
      mutation.put(
        new Text(layerId.name), new Text(),
        System.currentTimeMillis(),
        new Value(tile.toBytes())
      )
      
      (null, mutation)
    }

  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[SpatialKey] = {
    val LayerMetaData(layerId, rasterMetaData) = metaData
    val tileRdd = 
      rdd.map { case (key, value) =>
        val rowIdRx(_, col, row) = key.getRow.toString
        val tile = 
          ArrayTile.fromBytes(
            value.get,
            rasterMetaData.cellType, 
            rasterMetaData.tileLayout.pixelCols, 
            rasterMetaData.tileLayout.pixelRows
          )

        SpatialKey(col.toInt, row.toInt) -> tile.asInstanceOf[Tile]
    }

    new RasterRDD(tileRdd, rasterMetaData)
  }

  def setZoomBounds(job: Job, metaData: LayerMetaData): Unit = {
    val range = new ARange(
      new Text(s"${metaData.id.zoom}_0"),
      new Text(s"${metaData.id.zoom}_9")
    ) :: Nil

    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, layer: String, metaData: LayerMetaData, filterSet: FilterSet[SpatialKey]): Unit = {
    var tileBoundSet = false

    for(filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds, scheme) =>
          tileBoundSet = true

          val ranges =
            for(row <- bounds.rowMin to bounds.rowMax) yeild {
              new ARange(rowId(layerId, bounds.colMin, row), rowId(layerId, bounds.colMax, row))
            }

          InputFormatBase.setRanges(job, ranges)
      }
    }

    if (!tileBoundSet) setZoomBounds(job, metaData)

    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new JPair(new Text(layer), null: Text) :: Nil)
  }
}
