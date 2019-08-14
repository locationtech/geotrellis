package geotrellis.spark.testkit

import org.scalatest._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.layer.{SpatialKey, Bounds, LayoutDefinition}
import geotrellis.raster.testkit.RasterMatchers

import matchers._

trait LayoutRasterMatchers {
  self: Matchers with FunSpec with RasterMatchers =>
  def containKey(key: SpatialKey) = Matcher[Bounds[SpatialKey]] { bounds =>
    MatchResult(bounds.includes(key),
      s"""$bounds does not contain $key""",
      s"""$bounds contains $key""")
  }

  def withGeoTiffClue[T](
    key: SpatialKey,
    layout: LayoutDefinition,
    actual: MultibandTile,
    expect: MultibandTile,
    crs: CRS
  )(fun: => T): T = {
    val extent = layout.mapTransform.keyToExtent(key)
    withGeoTiffClue[T](Raster(actual, extent), Raster(expect, extent), crs)(fun)
  }
}
