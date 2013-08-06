package geotrellis.raster
  
import geotrellis._

trait RasterSourceBuilder[A] extends SourceBuilder[Raster,A] {
  def setRasterDefinition(dfn:Op[RasterDefinition]):this.type
}
 //class RasterSourceBuilder extends SourceBuilder[Raster, RasterSource] {
    //def result = new RasterSource(this.op)
 //}
