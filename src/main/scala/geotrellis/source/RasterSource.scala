package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

object RasterSource {
  implicit def foo =  new CanBuildSourceFrom[RasterSource, Raster, RasterSource] {
      def apply() = new RasterSourceBuilder
      def apply(rasterSrc:RasterSource) =
        RasterSourceBuilder(rasterSrc)
  }
  
/*
  implicit def canBuildSourceFromHistogram:CanBuildSourceFrom[RasterSource,Histogram,SingleDataSource[Histogram,Histogram]] =
    new CanBuildSourceFrom[RasterSource,
                           Histogram,
                           SingleDataSource[Histogram,Histogram]] {
      def apply() = new SingleDataSourceBuilder[Histogram,Histogram]
      def apply(src:RasterSource) = new SingleDataSourceBuilder[Histogram,Histogram]
    }
 */
 
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

class RasterSource(val rasterDef: Op[RasterDefinition]) extends  RasterSourceLike[RasterSource] {
  def elements = rasterDef.map(_.tiles)
  val rasterDefinition = rasterDef
  def converge = ValueDataSource(get)
}
