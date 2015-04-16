package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
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

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.util.matching.Regex

object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    ZSpaceTimeKeyIndex.byYear

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

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpaceTimeKey], keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey]): Unit = {
    val spaceFilters = mutable.ListBuffer[GridBounds]()
    val timeFilters = mutable.ListBuffer[(DateTime, DateTime)]()

    filterSet.filters.foreach {
      case SpaceFilter(bounds) => 
        spaceFilters += bounds
      case TimeFilter(start, end) =>
        timeFilters += ( (start, end) )
    }

    if(spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey.spatialKey
      val maxKey = keyBounds.maxKey.spatialKey
      spaceFilters += GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row)
    }

    if(timeFilters.isEmpty) {
      val minKey = keyBounds.minKey.temporalKey
      val maxKey = keyBounds.maxKey.temporalKey
      timeFilters += ( (minKey.time, maxKey.time) )

    }

    InputFormatBase.setLogLevel(job, org.apache.log4j.Level.DEBUG)
    
    val ranges: Seq[ARange] = (
      for {
        bounds <- spaceFilters
        (timeStart, timeEnd) <- timeFilters
      } yield {
        val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
        val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)
        
        val ranges = index.indexRanges(p1, p2)

        ranges
          .map { case (min: Long, max: Long) =>

            val start = f"${layerId.zoom}%02d_${min}%019d"
            val end   = f"${layerId.zoom}%02d_${max}%019d"
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

  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filters: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = metaData
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)
        setFilters(job, layerId, filters, keyBounds, index)
        val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value])
        val tileRdd = 
          rdd.map { case (_, value) =>
            val (key, tileBytes) = KryoSerializer.deserialize[(SpaceTimeKey, Array[Byte])](value.get)
            val tile =
              ArrayTile.fromBytes(
                tileBytes,
                rasterMetaData.cellType,
                rasterMetaData.tileLayout.tileCols,
                rasterMetaData.tileLayout.tileRows
              )
            (key, tile: Tile)
          }

        new RasterRDD(tileRdd, rasterMetaData)
      }
    }
}
