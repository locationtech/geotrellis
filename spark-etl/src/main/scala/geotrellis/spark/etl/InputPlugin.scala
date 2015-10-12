package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster.{CellType, Tile}
import geotrellis.spark.RasterRDD
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import org.apache.spark.SparkContext
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



