package geotrellis.benchmark

import geotrellis.raster._
import geotrellis.raster.reproject._

import geotrellis.vector.Extent

import geotrellis.proj4._

import com.google.caliper.Param

trait InterpolationBenchmark extends OperationBenchmark {

  def interp: InterpolationMethod

  val name = "SBN_farm_mkt"

  @Param(Array("64", "128", "256", "512", "1024"))
  var size = 0

  var tile: Tile = null

  var extent: Extent = null

  override def setUp() {
    tile = get(loadRaster(name, size, size))
    extent = Extent(0, 0, size / 100.0, size / 100.0)
  }

  def timeInterpolation(reps: Int) = run(reps)(interpolate)

  def interpolate =
    tile.reproject(extent, LatLng, WebMercator, ReprojectOptions(interp, 0.0))

}
