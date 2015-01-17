package geotrellis.spark.io.accumulo

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Range => ARange, Key, Value, Mutation}
import org.apache.accumulo.core.util.{Pair => APair}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.joda.time.{DateTimeZone, DateTime}
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import scala.reflect._

object TimeRasterAccumuloDriver extends AccumuloDriver[SpaceTimeKey] {
  val rowIdRx = new Regex("""(\d+)_(\d+)_(\d+)_(\d+)""", "zoom", "col", "row", "year")

  def timeChunk(time: DateTime): String =
    time.getYear.toString

  def rowId(id: LayerId, key: SpaceTimeKey): String = {
    val SpaceTimeKey(SpatialKey(col, row), TemporalKey(time)) = key
    f"${id.zoom}%02d_${col}%06d_${row}%06d_${timeChunk(time)}"
  }

  def timeText(key: SpaceTimeKey): Text = 
    new Text(key.temporalKey.time.withZone(DateTimeZone.UTC).toString)

  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(layerId: LayerId, raster: RasterRDD[SpaceTimeKey]): RDD[(Text, Mutation)] =
    raster.map {
      case (key, tile) =>    
        val mutation = new Mutation(rowId(layerId, key))
        mutation.put(new Text(layerId.name), timeText(key),
          System.currentTimeMillis(), new Value(tile.toBytes()))
        (null, mutation)
    }

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: RasterMetaData): RasterRDD[SpaceTimeKey] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(zoom, col, row, _) = key.getRow.toString
        val spatialKey = SpatialKey(col.toInt, row.toInt)
        val time = DateTime.parse(key.getColumnQualifier.toString)
        val tile = ArrayTile.fromBytes(value.get, metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
        SpaceTimeKey(spatialKey, time) -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }

  def loadTile(accumulo: AccumuloInstance)(layerId: LayerId, metaData: RasterMetaData, table: String, key: SpaceTimeKey): Tile = {
    val scanner  = accumulo.connector.createScanner(table, new Authorizations())
    scanner.setRange(new ARange(rowId(layerId, key)))
    scanner.fetchColumn(new Text(layerId.name), timeText(key))
    val values = scanner.iterator.toList.map(_.getValue)
    val value = 
      if(values.size == 0) {
        sys.error(s"Tile with key $key not found for layer $layerId")
      } else if(values.size > 1) {
        sys.error(s"Multiple tiles found for $key for layer $layerId")
      } else {
        values.head
      }

    ArrayTile.fromBytes(
      value.get,
      metaData.cellType,
      metaData.tileLayout.tileCols,
      metaData.tileLayout.tileRows
    )
  }

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

    assert(timeFilters.length == 1, "Only one TimeFilter supported at this time")

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
}
