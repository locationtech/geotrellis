package geotrellis.source
  
import geotrellis._
import geotrellis.raster._
import geotrellis.process.LayerId

class RasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  private var _dataDefinition:Op[RasterDefinition] = null
  private var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this 
  }

  def setRasterDefinition(dfn: Op[RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }

  def result = new RasterSource(_dataDefinition,_ops)
}

object RasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}

/** Builder for a RasterSource where the RasterDefinition is not known,
  * and requires evaluation of the single Raster inside the tiles.
  * Should only be used for in memory rasters, i.e. mapping a ValueSource
  * to a RasterSource.
  */
class BareRasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  private var _dataDefinition:Op[RasterDefinition] = null
  private var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this
  }

  def setRasterDefinition(dfn: Op[RasterDefinition]): this.type = 
    sys.error("Shouldn't be setting the RasterDefinition of a BareRasterSourceBuilder, use RasterSourceBuilder instead.")

  def result = {
    val rasterDefinition = 
      for(seq <- _ops;
          r <- seq.head) yield {
        RasterDefinition.fromRaster(r)
      }
    new RasterSource(rasterDefinition,_ops)
  }
}
