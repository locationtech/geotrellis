package geotrellis.spark.etl.hadoop

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.ingest._
import geotrellis.spark.reproject._
import geotrellis.spark.tiling.LayoutScheme
import geotrellis.spark.{RasterRDD, RasterMetaData, SpatialKey}
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel

import scala.reflect._
import geotrellis.spark.io.hadoop._

class GeoTiffHadoopInput extends HadoopInput {
  val format = "geotiff"
  val key = classTag[SpatialKey]

  def apply[K](lvl: StorageLevel, crs: CRS, layoutScheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {
    val source = sc.hadoopGeoTiffRDD(props("path"))
    val reprojected = source.reproject(crs).persist(lvl)
    val (layoutLevel, rasterMetaData) =
      RasterMetaData.fromRdd(reprojected, crs, layoutScheme) { _.extent }
    val tiler = implicitly[Tiler[ProjectedExtent, SpatialKey, Tile]]
    layoutLevel -> tiler(reprojected, rasterMetaData, NearestNeighbor).asInstanceOf[RasterRDD[K]]
  }
}

