package geotrellis.spark.etl.accumulo

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.tiling.{MapKeyTransform, LayoutScheme}
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import scala.reflect._

class SpaceTimeAccumuloInput extends AccumuloInput {

  def key = classTag[SpaceTimeKey]

  def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {
    val (id, bbox) = parse(props)
    val reader = AccumuloLayerReader[SpaceTimeKey, Tile, RasterRDD](getInstance(props))


    val rdd = bbox match {
      case Some(extent) => reader.query(id).where(Intersects(extent)).toRDD
      case None => reader.read(id)
    }

    id.zoom -> rdd.asInstanceOf[RasterRDD[K]]
  }
}
