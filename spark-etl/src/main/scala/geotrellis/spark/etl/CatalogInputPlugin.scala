package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.vector.Extent
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel

abstract class CatalogInputPlugin[K: SpatialComponent: Boundable] extends InputPlugin[K] {
  def format = "catalog"
  def requiredKeys: Array[String] = Array("layer")
  def reader(props: Parameters)(implicit sc: SparkContext): FilteringLayerReader[LayerId, K, RasterMetaData, RasterRDD[K]]

  def parse(props: Parameters): (LayerId, Option[Extent]) = {
    val bbox = props.get("bbox").map(Extent.fromString)
    val chunks = props("layer").split(":")
    val id = LayerId(chunks(0), chunks(1).toInt)
    (id, bbox)
  }

  def apply(
    lvl: StorageLevel,
    crs: CRS, scheme: Either[LayoutScheme, LayoutDefinition],
    targetCellType: Option[CellType],
    props: Parameters)
  (implicit sc: SparkContext): (Int, RasterRDD[K]) = {
    val (id, boundingBox) = parse(props)

    val rdd = boundingBox match {
      case Some(extent) =>
        reader(props).query(id).where(Intersects(extent)).toRDD
      case None =>
        reader(props).read(id)
    }

    id.zoom -> rdd
  }
}
