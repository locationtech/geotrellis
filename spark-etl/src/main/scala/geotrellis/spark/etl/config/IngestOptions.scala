package geotrellis.spark.etl.config

import geotrellis.proj4.CRS
import geotrellis.raster.resample.PointResampleMethod
import geotrellis.raster.{CellSize, CellType, RasterExtent}
import geotrellis.spark.io.index.{HilbertKeyIndexMethod, KeyIndexMethod, RowMajorKeyIndexMethod, ZCurveKeyIndexMethod}
import geotrellis.spark.tiling._
import geotrellis.vector.Extent

case class IngestOptions(
  resampleMethod: PointResampleMethod,
  reprojectMethod: ReprojectMethod,
  keyIndexMethod: IngestKeyIndexMethod,
  tileSize: Int = 256,
  pyramid: Boolean = true,
  layoutScheme: Option[String] = None,
  layoutExtent: Option[Extent] = None,
  crs: Option[CRS] = None,
  resolutionThreshold: Option[Double] = None,
  cellSize: Option[CellSize] = None,
  cellType: Option[CellType] = None,
  encoding: Option[String] = None,
  breaks: Option[String] = None
) {
  def getLayoutScheme: LayoutScheme = (layoutScheme, crs, resolutionThreshold) match {
    case (Some("floating"), _, _)            => FloatingLayoutScheme(tileSize)
    case (Some("zoomed"), Some(c), Some(rt)) => ZoomedLayoutScheme(c, tileSize, rt)
    case _ => throw new Exception("unsupported layout scheme definition")
  }

  def getLayoutDefinition = (layoutExtent, cellSize) match {
    case (Some(le), Some(cs)) => LayoutDefinition(RasterExtent(le, cs), tileSize)
    case _ => throw new Exception("unsupported layout definition")
  }

  private def _getKeyIndexMethod: KeyIndexMethod[_] = (keyIndexMethod.`type`, keyIndexMethod.temporalResolution) match {
    case ("rowmajor", None)    => RowMajorKeyIndexMethod
    case ("hilbert", None)     => HilbertKeyIndexMethod
    case ("hilbert", Some(tr)) => HilbertKeyIndexMethod(tr.toInt)
    case ("zorder", None)      => ZCurveKeyIndexMethod
    case ("zorder", Some(tr))  => ZCurveKeyIndexMethod.byMilliseconds(tr)
    case _                     => throw new Exception("unsupported keyIndexMethod definition")
  }

  def getKeyIndexMethod[K] = _getKeyIndexMethod.asInstanceOf[KeyIndexMethod[K]]
}
