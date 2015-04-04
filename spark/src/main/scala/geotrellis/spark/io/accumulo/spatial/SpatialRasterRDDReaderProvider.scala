package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.mapreduce.{ AccumuloInputFormat, InputFormatBase }
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpatialRasterRDDIndex {
  val rowIdRx = """(\d+)_(\d+)_(\d+)""".r // (zoom)_(col)_(row)

  def rowId(id: LayerId, key: SpatialKey): String = {
    val SpatialKey(col, row) = key    
    f"${id.zoom}%02d_${col}%06d_${row}%06d"
  }
}

object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {
  import SpatialRasterRDDIndex._

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpatialKey]): Unit = {
    var tileBoundSet = false

    for(filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          val ranges =
            for(row <- bounds.rowMin to bounds.rowMax) yield {
              new ARange(rowId(layerId, SpatialKey(bounds.colMin, row)), rowId(layerId, SpatialKey(bounds.colMax, row)))
            }

          InputFormatBase.setRanges(job, ranges)
      }
    }

    if (!tileBoundSet) setZoomBounds(job, layerId)

    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }

  def setZoomBounds(job: Job, layerId: LayerId): Unit = {
    val range = new ARange(
      new Text(f"${layerId.zoom}%02d"),
      new Text(f"${layerId.zoom+1}%02d")
    ) :: Nil

    InputFormatBase.setRanges(job, range)
  }

  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData)(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filters: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = metaData
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)
        setFilters(job, layerId, filters)
        val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
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
    }
}
