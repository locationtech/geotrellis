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
    new RasterSource(
      io.LoadRasterLayerInfo(name).map { info =>
        RasterDefinition(
          info.rasterExtent,
          info.tileLayout,
          (for(tileCol <- 0 until info.tileLayout.tileCols;
            tileRow <- 0 until info.tileLayout.tileRows) yield {
            io.LoadTile(name,tileCol,tileRow)
          }).toSeq
        )
      }
    )
}
