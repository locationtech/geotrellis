package geotrellis.raster
  
import geotrellis._

 class RasterSourceBuilder extends SourceBuilder[Raster, RasterSource] {
    def result = new RasterSource(this.op)
 }
