package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.raster._
import geotrellis.index.zcurve._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import org.joda.time.{DateTimeZone, DateTime}

import scala.collection.JavaConversions._
import scala.util.matching.Regex

object SpaceTimeRasterRDDIndex {
  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def timeChunk(time: DateTime): String =
    time.getYear.toString

  def rowId(id: LayerId, zindex: Z3): String =
    f"${id.zoom}%02d_${zindex.z}%019d"

  def rowId(id: LayerId, key: SpaceTimeKey): String = {
    val SpaceTimeKey(SpatialKey(col, row), TemporalKey(time)) = key
    val t = timeChunk(time).toInt
    rowId(id, Z3(col, row, t))
  }
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
  
  def timeSlugs(filters: List[(DateTime, DateTime)], minTime: DateTime, maxTime: DateTime): List[(Int, Int)] = filters match {
    case Nil =>
      List(timeChunk(minTime).toInt -> timeChunk(maxTime).toInt)
    case List((start, end)) =>                 
      List(timeChunk(start).toInt -> timeChunk(end).toInt)
  }

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpaceTimeKey], keyBounds: KeyBounds[SpaceTimeKey]): Unit = {
    var spaceFilters: List[GridBounds] = Nil
    var timeFilters: List[(DateTime, DateTime)] = Nil

    filterSet.filters.foreach {
      case SpaceFilter(bounds) => 
        spaceFilters = bounds :: spaceFilters
      case TimeFilter(start, end) =>
        timeFilters = (start, end) :: timeFilters
    }

    def timeIndex(dt: DateTime) = timeChunk(dt).toInt

    InputFormatBase.setLogLevel(job, org.apache.log4j.Level.DEBUG)
    
    val ranges: Seq[ARange] = (
      for {
        bounds <- spaceFilters
        (timeStart, timeEnd) <- timeSlugs(timeFilters, keyBounds.minKey.temporalKey.time, keyBounds.maxKey.temporalKey.time)
      } yield {
        val p1 = Z3(bounds.colMin, bounds.rowMin, timeStart)
        val p2 = Z3(bounds.colMax, bounds.rowMax, timeEnd)
        
        val rangeProps = Map(
          "min" -> p1.z.toString,
          "max" -> p2.z.toString)
        

        val ranges = Z3.zranges(p1, p2)

        ranges
          .map { case (min: Long, max: Long) =>

            val start = f"${layerId.zoom}%02d_${min}%019d"
            val end   = f"${layerId.zoom}%02d_${max}%019d"
            val zmin = new Z3(min)
            val zmax = new Z3(max)      
            if (min == max)
              ARange.exact(start)
            else
              new ARange(start, true, end, true)
          }        
      }).flatten

    InputFormatBase.setRanges(job, ranges)

    for ( (start, end) <- timeFilters) {
      val props =  Map(
        "startBound" -> start.toString,
        "endBound" -> end.toString,
        "startInclusive" -> "true",
        "endInclusive" -> "true"
      )

      InputFormatBase.addIterator(job, 
        new IteratorSetting(2, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props))
    }

    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }

  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = metaData
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)
        setFilters(job, layerId, filters, keyBounds)
        val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value])
        val tileRdd = 
          rdd.map { case (key, value) =>
            val rowIdRx(zoom, zindex) = key.getRow.toString
            val Z3(col, row, year) = new Z3(zindex.toLong)
            val spatialKey = SpatialKey(col, row)
            val time = DateTime.parse(key.getColumnQualifier.toString)
            val tile = ArrayTile.fromBytes(value.get, rasterMetaData.cellType, rasterMetaData.tileLayout.tileCols, rasterMetaData.tileLayout.tileRows)
            SpaceTimeKey(spatialKey, time) -> tile.asInstanceOf[Tile]
          }

        new RasterRDD(tileRdd, rasterMetaData)
      }
    }
}
