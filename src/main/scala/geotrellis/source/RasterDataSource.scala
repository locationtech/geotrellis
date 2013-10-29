package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

class RasterDataSource(val rasterDef: Op[RasterDefinition], val tileOps:Op[Seq[Op[Raster]]]) 
    extends  RasterDataSourceLike[RasterDataSource] {
  def elements = tileOps
  val rasterDefinition = rasterDef
}

object RasterDataSource {
  def apply(name:String):RasterDataSource =
    RasterDataSource(io.LoadRasterDefinition(name))

  def apply(rasterDef:Op[RasterDefinition]):RasterDataSource = {
    val tileOps = rasterDef.map { rd =>
      (for(tileCol <- 0 until rd.tileLayout.tileCols;
        tileRow <- 0 until rd.tileLayout.tileRows) yield {
        io.LoadTile(rd.layerName,tileCol,tileRow)
      })
    }
    new RasterDataSource(rasterDef, tileOps)
  }

  def apply(rasterDef:Op[RasterDefinition],tileOps:Op[Seq[Op[Raster]]]) =
    new RasterDataSource(rasterDef, tileOps)

  def apply(tiledRaster:TileRaster):RasterDataSource = {
    val rasterDef = 
      RasterDefinition("tiledRaster",
                       tiledRaster.rasterExtent,
                       tiledRaster.tileLayout)
    val tileOps = tiledRaster.tiles.map(Literal(_)) 
    new RasterDataSource(rasterDef, tileOps)
  }

  def apply(tiledRaster:Op[TileRaster])(implicit d:DI):RasterDataSource = {
    val rasterDef = tiledRaster.map { tr =>
      RasterDefinition("tiledRaster",
                       tr.rasterExtent,
                       tr.tileLayout)
    }
    val tileOps = tiledRaster.map(_.tiles.map(Literal(_)))
    new RasterDataSource(rasterDef, tileOps)
  }
}
