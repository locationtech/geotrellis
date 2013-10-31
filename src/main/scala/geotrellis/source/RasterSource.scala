package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

class RasterSource(val rasterDef: Op[RasterDefinition], val tileOps:Op[Seq[Op[Raster]]]) 
    extends  RasterSourceLike[RasterSource] {
  def elements = tileOps
  val rasterDefinition = rasterDef
}

object RasterSource {
  def apply(name:String):RasterSource =
    RasterSource(io.LoadRasterDefinition(name))

  def apply(rasterDef:Op[RasterDefinition]):RasterSource = {
    val tileOps = rasterDef.map { rd =>
      (for(tileCol <- 0 until rd.tileLayout.tileCols;
        tileRow <- 0 until rd.tileLayout.tileRows) yield {
        io.LoadTile(rd.layerName,tileCol,tileRow)
      })
    }
    new RasterSource(rasterDef, tileOps)
  }

  def apply(rasterDef:Op[RasterDefinition],tileOps:Op[Seq[Op[Raster]]]) =
    new RasterSource(rasterDef, tileOps)

  def apply(tiledRaster:TileRaster):RasterSource = {
    val rasterDef = 
      RasterDefinition("tiledRaster",
                       tiledRaster.rasterExtent,
                       tiledRaster.tileLayout)
    val tileOps = tiledRaster.tiles.map(Literal(_)) 
    new RasterSource(rasterDef, tileOps)
  }

  def apply(tiledRaster:Op[TileRaster])(implicit d:DI):RasterSource = {
    val rasterDef = tiledRaster.map { tr =>
      RasterDefinition("tiledRaster",
                       tr.rasterExtent,
                       tr.tileLayout)
    }
    val tileOps = tiledRaster.map(_.tiles.map(Literal(_)))
    new RasterSource(rasterDef, tileOps)
  }
}
