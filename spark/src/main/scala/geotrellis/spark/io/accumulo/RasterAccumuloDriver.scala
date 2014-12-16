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
  def rowId(layerId: LayerId, col: Int, row: Int) = new Text(f"${layerId.zoom}%02d_${col}%06d_${row}%06d")

  def rowId(id: LayerId, key: SpatialKey): String = {
    val SpatialKey(col, row) = key    
    f"${id.zoom}%02d_${col}%06d_${row}%06d"
  }

  def encode(layerId: LayerId, raster: RasterRDD[SpatialKey]): RDD[(Text, Mutation)] =
    raster.map { case (key, tile) =>
      val mutation = new Mutation(rowId(layerId, key.col, key.row))
      mutation.put(
        new Text(layerId.name), new Text(),
        System.currentTimeMillis(),
        new Value(tile.toBytes())
      )
      
      (null, mutation)
    }

  def decode(rdd: RDD[(Key, Value)], rasterMetaData: RasterMetaData): RasterRDD[SpatialKey] = {
    val tileRdd = 
      rdd.map { case (key, value) =>
        val rowIdRx(_, col, row) = key.getRow.toString
        val tile = 
          ArrayTile.fromBytes(
            value.get,
            rasterMetaData.cellType, 
            rasterMetaData.tileLayout.tileCols,
            rasterMetaData.tileLayout.tileRows
          )

        SpatialKey(col.toInt, row.toInt) -> tile.asInstanceOf[Tile]
    }

    new RasterRDD(tileRdd, rasterMetaData)
  }

  def setZoomBounds(job: Job, layerId: LayerId): Unit = {
    val range = new ARange(
      new Text(f"${layerId.zoom}%02d"),
      new Text(f"${layerId.zoom+1}%02d")
    ) :: Nil

    InputFormatBase.setRanges(job, range)
  }

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpatialKey]): Unit = {
    var tileBoundSet = false

    for(filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          val ranges =
            for(row <- bounds.rowMin to bounds.rowMax) yield {
              new ARange(rowId(layerId, bounds.colMin, row), rowId(layerId, bounds.colMax, row))
            }

          InputFormatBase.setRanges(job, ranges)
      }
    }

    if (!tileBoundSet) setZoomBounds(job, layerId)

    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new JPair(new Text(layerId.name), null: Text) :: Nil)
  }

  def getSplits(id: LayerId, rdd: RasterRDD[SpatialKey], num: Int = 24): Seq[String] = {
    import org.apache.spark.SparkContext._

    rdd
      .map( row => rowId(id, row._1) -> null)
      .sortByKey()
      .map(_._1)
      .repartition(num)
      .mapPartitions{ iter => iter.take(1) }
      .collect
  }
}
