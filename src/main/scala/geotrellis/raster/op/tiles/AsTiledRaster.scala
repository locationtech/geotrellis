package geotrellis.raster.op.tiles

import geotrellis._
import geotrellis.raster._

// case class AsTiledRaster(r: Op[Raster], pixelCols:Op[Int], pixelRows:Op[Int]) 
//      extends Op3(r,pixelCols,pixelRows)({
//   (r,pixelCols,pixelRows) =>
//      val tileLayout = Tiler.buildTileLayout(r.rasterExtent, pixelCols, pixelRows)
//      Result(Raster(LazyTiledWrapper(r.data, tileLayout), r.rasterExtent))
// })
