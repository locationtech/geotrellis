package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import scala.collection.mutable
import org.joda.time.{DateTimeZone, DateTime}

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] with Logging {

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] = {
    val lnOf2 = scala.math.log(2) // natural log of 2
    def log2(x: Double): Double = scala.math.log(x) / lnOf2
    val spatialResolution = log2(tileLayout.layoutCols).toInt

    new HilbertSpaceTimeKeyIndex(keyBounds.minKey, keyBounds.maxKey, spatialResolution, 8)
  }

  class SpaceTimeFilterMapFileInputFormat extends FilterMapFileInputFormat[SpaceTimeKey, SpaceTimeKeyWritable, TileWritable] {
    def createKey() = new SpaceTimeKeyWritable
    def createKey(index: Long) = SpaceTimeKeyWritable(index, SpaceTimeKey(0, 0, new DateTime(0,0,0)))
    def createValue() = new TileWritable
  }

  def reader(
    catalogConfig: HadoopRasterCatalogConfig, 
    layerMetaData: HadoopLayerMetaData, 
    keyIndex: KeyIndex[SpaceTimeKey], 
    keyBounds: KeyBounds[SpaceTimeKey]
  )(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def filterDefinition(filterSet: FilterSet[SpaceTimeKey]): FilterMapFileInputFormat.FilterDefinition[SpaceTimeKey] = {
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

        val indexRanges = 
          (for {
            bounds <- spaceFilters
            (timeStart, timeEnd) <- timeFilters
          } yield {
            val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
            val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)

            val i1 = keyIndex.toIndex(p1)
            val i2 = keyIndex.toIndex(p2)
            
            keyIndex.indexRanges((p1, p2))
          }).flatten.toArray

        (filterSet, indexRanges)
      }

      def read(layerId: LayerId, filterSet: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(SpaceTimeKeyWritable, TileWritable)] =
          if(filterSet.isEmpty) {
            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SequenceFileInputFormat[SpaceTimeKeyWritable, TileWritable]],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          } else {
            inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition(filterSet))

            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SpaceTimeFilterMapFileInputFormat],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          }

        val rasterMetaData = layerMetaData.rasterMetaData

        asRasterRDD(rasterMetaData) {
          writableRdd
            .map  { case (keyWritable, tileWritable) =>
              (keyWritable.get._2, tileWritable.toTile(rasterMetaData))
          }
        }

      }
    }
}
