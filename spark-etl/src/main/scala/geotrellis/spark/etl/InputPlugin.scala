package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.spark.RasterRDD
import geotrellis.spark.tiling.{LayoutLevel, LayoutScheme}
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel

import scala.reflect._

trait InputPlugin {
  def name: String
  def format: String
  def key: ClassTag[_]
  def requiredKeys: Array[String]

  def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext): (Int, RasterRDD[K])

  def validate(props: Map[String, String]) =
    requireKeys(name, props, requiredKeys)

  def suitableFor(name: String, format: String, keyClassTag: ClassTag[_]): Boolean =
    (name.toLowerCase, format.toLowerCase, keyClassTag) == (this.name, this.format, this.key)
}