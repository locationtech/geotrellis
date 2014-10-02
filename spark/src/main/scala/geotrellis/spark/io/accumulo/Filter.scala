package geotrellis.spark.io.accumulo

import geotrellis.raster.GridBounds
import geotrellis.spark.ingest.IngestNetCDF.TimeBandTile
import geotrellis.spark.tiling.{GridCoordScheme, TileCoordScheme}
import geotrellis.spark.{TileBounds, TileId}

trait AccumuloFilter

trait SpacialFilter extends AccumuloFilter {
  val bounds: TileBounds
  val scheme: TileCoordScheme
}

trait TemporalFilter extends AccumuloFilter

case class SpaceFilter(bounds: TileBounds, scheme: TileCoordScheme) extends SpacialFilter

case class TimeFilter(startTime: Double, endTime: Double) extends TemporalFilter
