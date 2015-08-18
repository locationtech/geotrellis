package geotrellis.spark.etl.accumulo

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.tiling.{LayoutLevel, LayoutScheme}
import geotrellis.vector.Extent
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import scala.reflect._

class SpatialAccumuloInput extends AccumuloInput {

  def key = classTag[SpatialKey]

  def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {

    val bbox = props.get("bbox").map(Extent.fromString)
    val chunks = props("layer").split(":")
    val id = LayerId(chunks(0), chunks(1).toInt)
    val catalog = AccumuloRasterCatalog()(getInstance(props), sc)

    val rdd = bbox match {
      case Some(extent) => catalog.query[SpatialKey](id).where(Intersects(extent)).toRDD
      case None => catalog.read[SpatialKey](id)
    }

    LayoutLevel(id.zoom, rdd.metaData.tileLayout) -> rdd.asInstanceOf[RasterRDD[K]]
  }

}
