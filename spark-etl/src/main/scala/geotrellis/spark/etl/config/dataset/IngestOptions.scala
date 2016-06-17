package geotrellis.spark.etl.config.dataset

import geotrellis.proj4.CRS
import geotrellis.raster.{CellSize, CellType, RasterExtent}
import geotrellis.raster.resample.PointResampleMethod
import geotrellis.spark.etl.ReprojectMethod
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutScheme, ZoomedLayoutScheme}
import geotrellis.vector.Extent

case class IngestOptions(
  resampleMethod: PointResampleMethod,
  layoutScheme: Option[String],
  layoutExtent: Option[Extent],
  crs: Option[CRS],
  tileSize: Int,
  resolutionThreshold: Option[Double],
  cellSize: Option[CellSize],
  cellType: Option[CellType],
  pyramid: Boolean,
  reprojectMethod: ReprojectMethod,
  keyIndexMethod: IngestKeyIndexMethod
) {
  lazy val getLayoutScheme: LayoutScheme = (layoutScheme, crs, resolutionThreshold) match {
    case (Some("floating"), _, _)            => FloatingLayoutScheme(tileSize)
    case (Some("zoomed"), Some(c), Some(rt)) => ZoomedLayoutScheme(c, tileSize, rt)
    case _ => throw new Exception("unsupported layout scheme definition")
  }

  lazy val getLayoutDefinition = (layoutExtent, cellSize) match {
    case (Some(le), Some(cs)) => LayoutDefinition(RasterExtent(le, cs), tileSize)
    case _ => throw new Exception("unsupported layout definition")
  }
}
