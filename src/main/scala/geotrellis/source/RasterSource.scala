package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

class RasterSource(val rasterDef: Op[RasterDefinition]) extends  RasterSourceLike[RasterSource] {
  def elements = rasterDef.map(_.tiles)
  val rasterDefinition = rasterDef
  def converge = ValueDataSource(get)
}

object RasterSource {
 def apply(name:String):RasterSource =
    new RasterSource(io.LoadRasterDefinition(name))
}
