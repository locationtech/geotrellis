package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.{ AccumuloInputFormat, InputFormatBase }
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import org.joda.time.{DateTimeZone, DateTime}

import scala.collection.JavaConversions._
import scala.util.matching.Regex

object SpaceTimeRasterRDDIndex {
  val rowIdRx = new Regex("""(\d+)_(\d+)_(\d+)_(\d+)""", "zoom", "col", "row", "year")

  def timeChunk(time: DateTime): String =
    time.getYear.toString

  def rowId(id: LayerId, key: SpaceTimeKey): String = {
    val SpaceTimeKey(SpatialKey(col, row), TemporalKey(time)) = key
    f"${id.zoom}%02d_${col}%06d_${row}%06d_${timeChunk(time)}"
  }

  def timeText(key: SpaceTimeKey): Text = 
    new Text(key.temporalKey.time.withZone(DateTimeZone.UTC).toString)
}

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

  def tileSlugs(filters: List[GridBounds]): List[(String, String)] = filters match {
    case Nil =>
      List(("0"*6 + "_" + "0"*6) -> ("9"*6 + "_" + "9"*6))
    case _ => 
      for{
        bounds <- filters
        row <- bounds.rowMin to bounds.rowMax 
      } yield f"${bounds.colMin}%06d_${row}%06d" -> f"${bounds.colMax}%06d_${row}%06d"      
  }
  
  def timeSlugs(filters: List[(DateTime, DateTime)]): List[(String, String)] = filters match {
    case Nil =>
      List(("0"*4) -> ("9"*4))
    case List((start, end)) =>                 
      List(timeChunk(start) -> timeChunk(end))
  }

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpaceTimeKey]): Unit = {
    var spaceFilters: List[GridBounds] = Nil
    var timeFilters: List[(DateTime, DateTime)] = Nil

    filterSet.filters.foreach {
      case SpaceFilter(bounds) => 
        spaceFilters = bounds :: spaceFilters
      case TimeFilter(start, end) =>
        timeFilters = (start, end) :: timeFilters
    }

    val ranges = for {
      (tileFrom, tileTo) <- tileSlugs(spaceFilters)
      (timeFrom, timeTo) <- timeSlugs(timeFilters)
    } yield {
      val start = f"${layerId.zoom}%02d_${tileFrom}_${timeFrom}"
      val end   = f"${layerId.zoom}%02d_${tileTo}_${timeTo}"
      new ARange(start, end)
    }

    InputFormatBase.setRanges(job, ranges)

    assert(timeFilters.length <= 1, "Only one TimeFilter supported at this time")

    for ( (start, end) <- timeFilters) {
      val props =  Map(
        "startBound" -> start.toString,
        "endBound" -> end.toString,
        "startInclusive" -> "true",
        "endInclusive" -> "true"
      )

      InputFormatBase.addIterator(job, 
        new IteratorSetting(1, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props))
    }

    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }

  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData)(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val AccumuloLayerMetaData(layerMetaData, tileTable) = metaData
        val rasterMetaData = layerMetaData.rasterMetaData
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)
        setFilters(job, layerId, filters)
        val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
        val tileRdd = rdd.map {
          case (key, value) =>
            val rowIdRx(zoom, col, row, _) = key.getRow.toString
            val spatialKey = SpatialKey(col.toInt, row.toInt)
            val time = DateTime.parse(key.getColumnQualifier.toString)
            val tile = ArrayTile.fromBytes(value.get, rasterMetaData.cellType, rasterMetaData.tileLayout.tileCols, rasterMetaData.tileLayout.tileRows)
            SpaceTimeKey(spatialKey, time) -> tile.asInstanceOf[Tile]
        }

        new RasterRDD(tileRdd, rasterMetaData)
      }
    }
}
