package geotrellis.raster.rasterize

import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.raster.{ArrayTile, CellType, DoubleConstantNoDataCellType, RasterExtent}
import geotrellis.util.MethodExtensions
import geotrellis.vector.triangulation.{DelaunayTriangulation, StitchedDelaunay}

/**
  * Created by daunnc on 14/07/2017.
  */
trait StitchedDelaunayRasterizeMethods extends MethodExtensions[StitchedDelaunay] {
  def rasterize(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(center: DelaunayTriangulation) = {
    val tile = ArrayTile.empty(cellType, re.cols, re.rows)
    DelaunayRasterizer.rasterizeDelaunayTriangulation(center, re, tile)
    DelaunayRasterizer.rasterize(
      tile,
      re,
      self.fillTriangles,
      self.halfEdgeTable,
      self.pointSet
    )
    tile
  }
}
