package geotrellis.raster.reproject

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.BenchmarkParams

import geotrellis.bench.init
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector._

import scala.util.Random

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class RasterizingReprojectBench  {
  import RasterizingReprojectBench._

  @Param(Array("32", "64", "128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = _

  var raster: ProjectedRaster[Tile] = _
  var destRE: ProjectedRasterExtent = _

  val transform: Transform = Transform(srcCrs, destCrs)
  val inverse: Transform = Transform(destCrs, srcCrs)

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams): Unit = {
    val len = size * size
    raster = ProjectedRaster(ArrayTile(init(len)(Random.nextInt), size, size), srcExtent, srcCrs)
    destRE = ProjectedRasterExtent(raster.projectedExtent.reproject(destCrs), destCrs, size, size)
  }

  @Benchmark
  def standardReproject: Raster[Tile] = {
    raster.raster.reproject(destRE: RasterExtent, transform, inverse)
  }

  @Benchmark
  def rasterizingReproject: ProjectedRaster[Tile] = {
    raster.regionReproject(destRE, NearestNeighbor)
  }
}

object RasterizingReprojectBench {
  val srcCrs = LatLng
  val destCrs = ConusAlbers
  val srcExtent = Extent(-109, 37, -102, 41)
}
