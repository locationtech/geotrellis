package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellType, Tile, CellGrid}
import geotrellis.spark.etl.accumulo._
import geotrellis.spark.io.{Intersects, FilteringLayerReader, Reader}
import geotrellis.spark.io.accumulo.AccumuloLayerReader
import geotrellis.spark.reproject._
import geotrellis.spark.{SpaceTimeKey, LayerId, RasterMetaData, RasterRDD}
import geotrellis.spark.ingest._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.vector.Extent
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

trait InputPlugin[K] extends Serializable {
  def name: String
  def format: String
  def requiredKeys: Array[String]

  type V = Tile
  type Parameters = Map[String, String]

  def apply(
    lvl: StorageLevel,
    crs: CRS, scheme: Either[LayoutScheme, LayoutDefinition],
    targetCellType: Option[CellType],
    props: Parameters)
  (implicit sc: SparkContext): (Int, RasterRDD[K])

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String, format: String): Boolean =
    (name.toLowerCase, format.toLowerCase) == (this.name, this.format)
}



