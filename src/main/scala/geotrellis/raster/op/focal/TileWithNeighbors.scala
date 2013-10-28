package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

object TileWithNeighbors {
  def apply(r:Raster,neighbors:Seq[Option[Raster]]):(RasterLike,GridBounds) = 
    if(neighbors.isEmpty) {
      (r,GridBounds(0,0,r.rasterExtent.cols-1,r.rasterExtent.rows-1))
    } else {
      val re =
        neighbors.flatten
          .map(_.rasterExtent)
          .reduceLeft((re1,re2) => re1.combine(re2))

      val nw = neighbors(7)
      val n = neighbors(0)
      val ne = neighbors(1)
      val w = neighbors(6)
      val e = neighbors(2)
      val sw = neighbors(5)
      val s = neighbors(4)
      val se = neighbors(3)

      val westCol = 
        if(Seq(nw,w,sw).flatten.isEmpty) 0 else 1
      val eastCol = 
        if(Seq(ne,e,se).flatten.isEmpty) 0 else 1
      val northRow = 
        if(Seq(nw,n,ne).flatten.isEmpty) 0 else 1
      val southRow = 
        if(Seq(sw,s,se).flatten.isEmpty) 0 else 1

      val tileCols = 1 + westCol + eastCol
      val tileRows = 1 + northRow + southRow

      val tileLayout = TileLayout(tileCols, tileRows, r.rasterExtent.cols, r.rasterExtent.rows)

      // Determine the min/max index of the target raster inside of the
      // tiled raster (the analysis area).
      val colMin = if(westCol == 0) 0 else (neighbors(6).get.rasterExtent.cols)
      val colMax = colMin + r.rasterExtent.cols - 1
      val rowMin = if(northRow == 0) 0 else (neighbors(0).get.rasterExtent.rows)
      val rowMax = rowMin + r.rasterExtent.rows - 1

      val tiledRaster = 
        TileRaster(Seq(
          neighbors(7),
          neighbors(0),
          neighbors(1),
          neighbors(6),
          Some(r),
          neighbors(2),
          neighbors(5),
          neighbors(4),
          neighbors(3)
        ).flatten, re, tileLayout)
      (tiledRaster,GridBounds(colMin,rowMin,colMax,rowMax))
    }
}
