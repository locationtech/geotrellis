package geotrellis.benchmark

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._

import geotrellis.vector.Extent

import geotrellis.proj4._

import com.google.caliper.Param

trait ResampleBenchmark extends OperationBenchmark {

  def resamp: ResampleMethod

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

  def timeResample(reps: Int) = run(reps)(resample)

  def resample =
    tile.reproject(extent, LatLng, WebMercator, Reproject.Options(resamp, 0.0))

  def timeResampleDouble(reps: Int) = run(reps)(resampleDouble)

  def resampleDouble =
    tileDouble.reproject(extent, LatLng, WebMercator, Reproject.Options(resamp, 0.0))

}
