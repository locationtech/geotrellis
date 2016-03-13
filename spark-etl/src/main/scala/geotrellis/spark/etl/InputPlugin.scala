package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster.{CellType, Tile}
import geotrellis.spark.{Metadata, TileLayerRDD}
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

trait InputPlugin[I, V] extends Plugin with Serializable {
  def name: String
  def format: String

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String, format: String): Boolean =
    (name.toLowerCase, format.toLowerCase) == (this.name, this.format)

  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(I, V)]
}



