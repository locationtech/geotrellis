package geotrellis.benchmark

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.interpolation._

import geotrellis.vector.Extent

import geotrellis.proj4._

import com.google.caliper.Param

trait InterpolationBenchmark extends OperationBenchmark {

  def interp: InterpolationMethod

  val tileName = "SBN_farm_mkt"

  val tileDoubleName = "mtsthelens_tiled"

  @Param(Array("64", "128", "256", "512", "1024"))
  var size = 0

  var tile: Tile = null

  var tileDouble: Tile = null

  var extent: Extent = null

  override def setUp() {
    tile = get(loadRaster(tileName, size, size))
    tileDouble = get(loadRaster(tileDoubleName, size, size))
    extent = Extent(0, 0, size / 100.0, size / 100.0)
  }

  def timeInterpolation(reps: Int) = run(reps)(interpolate)

  def interpolate =
    tile.reproject(extent, LatLng, WebMercator, ReprojectOptions(interp, 0.0))

  def timeInterpolationDouble(reps: Int) = run(reps)(interpolateDouble)

  def interpolateDouble =
    tileDouble.reproject(extent, LatLng, WebMercator, ReprojectOptions(interp, 0.0))

}
