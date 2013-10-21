package geotrellis.source

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.Histogram
import geotrellis.raster._

class RasterDataSource(val rasterDef: Op[RasterDefinition], val tileOps:Op[Seq[Op[Raster]]]) extends  RasterDataSourceLike[RasterSource] {
  def elements = tileOps
  val rasterDefinition = rasterDef
}

object RasterSource {
  def apply(name:String):RasterDataSource =
    RasterSource(io.LoadRasterDefinition(name))

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
}
