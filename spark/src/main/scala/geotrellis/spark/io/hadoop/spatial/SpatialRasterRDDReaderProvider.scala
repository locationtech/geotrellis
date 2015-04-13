package geotrellis.spark.io.hadoop.spatial

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

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] with Logging {

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] =
    new RowMajorSpatialKeyIndex(tileLayout.layoutCols)

  class SpatialFilterMapFileInputFormat extends FilterMapFileInputFormat[SpatialKey, SpatialKeyWritable, TileWritable] {
    def createKey() = new SpatialKeyWritable
    def createKey(index: Long) = SpatialKeyWritable(index, SpatialKey(0, 0))
    def createValue() = new TileWritable
  }

  def reader(
    catalogConfig: HadoopRasterCatalogConfig, 
    layerMetaData: HadoopLayerMetaData, 
    keyIndex: KeyIndex[SpatialKey],
    keyBounds: KeyBounds[SpatialKey]
  )(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def filterDefinition(filterSet: FilterSet[SpatialKey]): FilterMapFileInputFormat.FilterDefinition[SpatialKey] = {
        val spaceFilters = mutable.ListBuffer[GridBounds]()

        filterSet.filters.foreach {
          case SpaceFilter(bounds) =>
            spaceFilters += bounds
        }

        if(spaceFilters.isEmpty) {
          val minKey = keyBounds.minKey
          val maxKey = keyBounds.maxKey
          spaceFilters += GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row)
        }

        val indexRanges = 
          (for {
            bounds <- spaceFilters
          } yield {
            val p1 = SpatialKey(bounds.colMin, bounds.rowMin)
            val p2 = SpatialKey(bounds.colMax, bounds.rowMax)

            val i1 = keyIndex.toIndex(p1)
            val i2 = keyIndex.toIndex(p2)
            
            keyIndex.indexRanges((p1, p2))
          }).flatten.toArray

        (filterSet, indexRanges)
      }

      def read(layerId: LayerId, filterSet: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(SpatialKeyWritable, TileWritable)] =

        if(filterSet.isEmpty) {
          sc.newAPIHadoopRDD(
            inputConf,
            classOf[SequenceFileInputFormat[SpatialKeyWritable, TileWritable]],
            classOf[SpatialKeyWritable],
            classOf[TileWritable]
          )
        } else {
          inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition(filterSet))

          sc.newAPIHadoopRDD(
            inputConf,
            classOf[SpatialFilterMapFileInputFormat],
            classOf[SpatialKeyWritable],
            classOf[TileWritable]
          )
        }

        val rasterMetaData = layerMetaData.rasterMetaData

        asRasterRDD(rasterMetaData) {
          writableRdd
            .map { case (keyWritable, tileWritable) =>
              (keyWritable.key, tileWritable.toTile(rasterMetaData))
          }
        }
      }
    }
}
