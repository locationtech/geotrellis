package geotrellis.raster.resample

import geotrellis.proj4.{LatLng, Transform, WebMercator}
import geotrellis.raster.{Raster, RasterExtent, Tile}
import org.openjdk.jmh.annotations.{Mode ⇒ JMHMode, _}
import geotrellis.bench._

@BenchmarkMode(Array(JMHMode.AverageTime))
@State(Scope.Thread)
@Threads(4)
class ResampleBench {
  @Param(Array("NearestNeighbor", "Bilinear", "CubicConvolution", "CubicSpline", "Lanczos", "Average", "Mode"  ))
  var methodName: String = _

  var method: ResampleMethod = _
  var raster: Raster[Tile] = _
  var rasterExtent: RasterExtent = _
  var trans: Transform = _
  var invTrans: Transform = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    val geotiff = readSinglebandGeoTiff("aspect-tif.tif")
    method = methodName match {
      case "NearestNeighbor" ⇒ NearestNeighbor
      case "Bilinear" ⇒ Bilinear
      case "CubicConvolution" ⇒ CubicConvolution
      case "CubicSpline" ⇒ CubicSpline
      case "Lanczos" ⇒ Lanczos
      case "Average" ⇒ Average
      case "Mode" ⇒ Mode
    }
    trans = Transform(LatLng, WebMercator)
    invTrans = Transform(WebMercator, LatLng)
    raster = geotiff.raster
    rasterExtent = raster.rasterExtent
  }

  @Benchmark
  def reproject = raster.reproject(rasterExtent, trans, invTrans, method)
}
