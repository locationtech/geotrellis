package geotrellis.spark.etl.s3

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.spark.ingest.{Tiler, SpaceTimeInputKey}
import geotrellis.spark.io.s3.TemporalGeoTiffS3InputFormat
import geotrellis.spark.tiling.LayoutScheme
import geotrellis.spark._
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import scala.reflect._

class TemporalGeoTiffS3Input extends S3Input {
  val format = "temporal-geotiff"
  val key = classTag[SpaceTimeKey]

  def apply[K](lvl: StorageLevel, crs: CRS, layoutScheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {
    val source = sc.newAPIHadoopRDD(configuration(props), classOf[TemporalGeoTiffS3InputFormat], classOf[SpaceTimeInputKey], classOf[Tile])
    val reprojected = source.reproject(crs).persist(lvl)
    val (layoutLevel, rasterMetaData) =
      RasterMetaData.fromRdd(reprojected, crs, layoutScheme) { _.extent }
    val tiler = implicitly[Tiler[SpaceTimeInputKey, SpaceTimeKey]]
    layoutLevel -> tiler(reprojected, rasterMetaData).asInstanceOf[RasterRDD[K]]
  }
}
